package br.org.cesar.knot_setup_app.view.gatewayConnected;


import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import br.org.cesar.knot_setup_app.data.DataManager;
import br.org.cesar.knot_setup_app.view.gatewayConnected.GatewayConnectedContract.Presenter;
import br.org.cesar.knot_setup_app.view.gatewayConnected.GatewayConnectedContract.ViewModel;
import br.org.cesar.knot_setup_app.utils.Constants;
import br.org.cesar.knot_setup_app.wrapper.LogWrapper;

import static br.org.cesar.knot_setup_app.utils.Constants.DNS_SD_SERVICE_TYPE;

public class GatewayConnectedPresenter implements Presenter {

    private ViewModel viewModel;
    private Context context;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private static DataManager dataManager;

    private ArrayList<NsdServiceInfo> service;

    GatewayConnectedPresenter(ViewModel viewModel, NsdManager nsdManager, Context context) {
        this.viewModel = viewModel;
        this.service = new ArrayList<NsdServiceInfo>();
        this.nsdManager = nsdManager;
        this.context = context;
    }

    private void getGateway(NsdServiceInfo nsdServiceInfo) {
        LogWrapper.Log("service: " + nsdServiceInfo, Log.DEBUG);
        nsdManager.resolveService(nsdServiceInfo,new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                LogWrapper.Log("Resolve failed: " + errorCode, Log.DEBUG);
                getGateway(serviceInfo);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {

                LogWrapper.Log("Resolve Succeeded. " + serviceInfo, Log.DEBUG);
                service.add(serviceInfo);
                viewModel.onGatewaysFound(service);
                if (serviceInfo.getServiceName().equals(Constants.DNS_SD_SERVICE_NAME)) {
                    LogWrapper.Log("Same IP.", Log.DEBUG);
                }
            }
        });
    }

    public void onGatewayClicked(NsdServiceInfo serviceInfo) {
        //stores the ip in the user preferences
        dataManager.getInstance().getPreference().
                setSharedPreferenceString(context,
                        Constants.GATEWAY_IP, serviceInfo.getHost().getHostAddress());
        //stores the port in the user preferences
        dataManager.getInstance().getPreference().
                setSharedPreferenceString(context,
                        Constants.GATEWAY_PORT, String.valueOf(serviceInfo.getPort()));
    }


    public void stopScanning() {
        LogWrapper.Log("Service discovery stopped.", Log.DEBUG);
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    @Override
    public void onFocusLost() {
        //empties the list
        service.clear();
        //stops mDNS
        this.stopScanning();
    }

    @Override
    public void onFocus() {

        // Instantiate a new DiscoveryListener
        if(discoveryListener == null) {

            discoveryListener = new NsdManager.DiscoveryListener() {
                // Called as soon as service discovery begins.
                @Override
                public void onDiscoveryStarted(String regType) {
                    LogWrapper.Log("Service discovery started", Log.DEBUG);
                }

                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    // A service was found! Do something with it.
                    if (!service.getServiceType().equals(DNS_SD_SERVICE_TYPE)) {
                        LogWrapper.Log("Service: " + service, Log.DEBUG);
                        removeFromListIfSameName(service);
                        if (service.getServiceName().startsWith(Constants.DNS_SD_SERVICE_NAME)) {
                            getGateway(service);
                        }
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo service) {
                    LogWrapper.Log("onServiceLost", Log.DEBUG);
                    removeFromListIfSameName(service);
                    viewModel.onGatewaysFound(GatewayConnectedPresenter.this.service);
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                    LogWrapper.Log("Discovery stopped: "
                            + serviceType, Log.DEBUG);
                }

                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                    LogWrapper.Log("Discovery failed: Error code:"
                            + errorCode, Log.DEBUG);
                }
                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                    LogWrapper.Log("Discovery failed: Error code:"
                            + errorCode, Log.DEBUG);
                }

            };

        }

        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);

    }

    private boolean removeFromListIfSameName(NsdServiceInfo service) {
        for (NsdServiceInfo info: this.service) {
            if(info.getServiceName().equals(service.getServiceName())) {
                this.service.remove(info);
                return true;
            }
        }
        feedbackOfGatewayListState();
        return false;
    }

    private void feedbackOfGatewayListState() {
        LogWrapper.Log("isMserviceEmpty: " + service.isEmpty(), Log.DEBUG);
        if(service.isEmpty()) {
            viewModel.setSearchingFeedback(View.INVISIBLE);
        } else {
            viewModel.setSearchingFeedback(View.VISIBLE);
        }

    }

}
