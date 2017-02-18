package github.tylerjmcbride.direct;

import android.app.Application;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.receivers.ClientBroadcastReceiver;

public class Client extends Direct {

    private WifiP2pDnsSdServiceRequest serviceRequest = null;
    private List<WifiP2pDevice> nearbyHosts = new ArrayList();
    private WifiP2pDevice host = null;
    boolean isDiscovering = false;

    /**
     * Creates a Wi-Fi Direct Client.
     */
    public Client(Application application, String service, int serverPort, String instance) {
        super(application, service, serverPort, instance);
        receiver = new ClientBroadcastReceiver(manager, channel, this);
    }

    /**
     * Starts the discovery of services.
     * @param listener
     */
    public void startDiscovery(final WifiP2pManager.ActionListener listener) {
        setDiscovering(true);
        setDnsSdResponseListeners();
        setServiceRequest(WifiP2pDnsSdServiceRequest.newInstance());

        manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to remove service request.");
                manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to add service request.");
                        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Succeeded to start service discovery.");
                                listener.onSuccess();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "Failed to start service discovery.");
                                listener.onFailure(reason);
                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "Failed to add service request.");
                        listener.onFailure(reason);
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to remove local service.");
                listener.onFailure(reason);
            }
        });
    }

    /**
     * Ends the discovery of servies.
     * @param listener
     */
    public void stopDiscovery(final WifiP2pManager.ActionListener listener) {
        manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to remove service request.");
                setServiceRequest(null);
                setDiscovering(false);
                listener.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Succeeded to remove service request.");
                listener.onSuccess();
            }
        });
    }

    /**
     * Connects to the specified host.
     * @param host The specified host.
     * @param listener The listener.
     */
    public void connect(final WifiP2pDevice host, final WifiP2pManager.ActionListener listener) {
        if(host != null) {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = host.deviceAddress;

            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Client.this.host = host;
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int reason) {
                    listener.onFailure(reason);
                }
            });
        }
    }

    /**
     * Creates and returns a deep copy of the list of nearby hosts.
     * @return A deep copy of the list of nearby hosts.
     */
    public List<WifiP2pDevice> getNearbyHosts() {
        List<WifiP2pDevice> deepNearbyHostsClone = new ArrayList();
        for(WifiP2pDevice device : nearbyHosts) {
            deepNearbyHostsClone.add(new WifiP2pDevice(device));
        }
        return deepNearbyHostsClone;
    }

    private void setDiscovering(boolean isDiscovering) {
        this.isDiscovering = isDiscovering;
    }

    /**
     * Sets the {@link WifiP2pDnsSdServiceRequest}.
     */
    private void setServiceRequest(WifiP2pDnsSdServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    /**
     * Sets the {@link WifiP2pManager} DNS response listeners.
     */
    private void setDnsSdResponseListeners() {
        manager.setDnsSdResponseListeners(channel, new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice device) {
                Log.d(TAG, "Found: " + device);
            }
        }, new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                if(record.containsKey(SERVICE_NAME_TAG) && record.get(SERVICE_NAME_TAG).equals(service)) {
                    if (!nearbyHosts.contains(device)) {
                        nearbyHosts.add(device);
                    }
                }
            }
        });
    }
}
