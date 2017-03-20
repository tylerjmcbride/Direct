package github.tylerjmcbride.direct;

import android.app.Application;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.model.Device;
import github.tylerjmcbride.direct.receivers.DirectBroadcastReceiver;
import github.tylerjmcbride.direct.registration.ClientRegistrar;

public class Client extends Direct {

    private ClientRegistrar registrar;
    private WifiP2pDnsSdServiceRequest serviceRequest = null;
    private Map<WifiP2pDevice, Integer> nearbyHostDevices = new HashMap<>();
    private Device hostDevice = null;
    private int hostPort;

    /**
     * Creates a Wi-Fi Direct Client.
     */
    public Client(Application application, String service, int serverPort, String instance) {
        super(application, service, serverPort, instance);
        receiver = new DirectBroadcastReceiver(manager, channel) {
            @Override
            protected void connectionChanged(WifiP2pInfo p2pInfo, NetworkInfo networkInfo, WifiP2pGroup p2pGroup) {
                if (hostDevice == null && p2pInfo.groupFormed && networkInfo.isConnected()) {
                    InetSocketAddress hostAddress = new InetSocketAddress(p2pInfo.groupOwnerAddress.getHostAddress(), hostPort);

                    registrar.register(thisDevice, hostAddress, new RegisteredWithServerListener() {
                        @Override
                        public void onSuccess(Device host) {
                            Log.d(TAG, "Succeeded to register with hostDevice.");
                            Client.this.hostDevice = host;
                        }

                        @Override
                        public void onFailure() {
                            Log.d(TAG, "Failed to register with hostDevice.");
                        }
                    });
                } else {
                    hostDevice = null;
                }
            }

            @Override
            protected void peersChanged() {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        nearbyPeers = peers;
                    }
                });
            }

            @Override
            protected void stateChanged(boolean wifiEnabled) {
                Log.e(TAG, "memes");
            }

            @Override
            protected void discoveryChanged(boolean discoveryEnabled) {
                Log.d(TAG, "Discovery changed.");
            }

            @Override
            protected void thisDeviceChanged(WifiP2pDevice device) {
                thisDevice = new Device(device.deviceName, device.deviceAddress, wifiManager.getConnectionInfo().getIpAddress(), 5445);
            }
        };
        application.getApplicationContext().registerReceiver(receiver, intentFilter);
        registrar = new ClientRegistrar(this, handler);
    }

    public void sendData() {
        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                InetSocketAddress hostAddress = new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), hostPort);


            }
        });
    }

    /**
     * Starts the discovery of services.
     * @param listener The listener.
     */
    public void startDiscovery(final WifiP2pManager.ActionListener listener) {
        if(serviceRequest == null) {
            setDnsSdResponseListeners();
            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

            manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Succeeded to remove service request.");
                    nearbyHostDevices.clear();
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
    }

    /**
     * Ends the discovery of servies.
     * @param listener The listener.
     */
    public void stopDiscovery(final WifiP2pManager.ActionListener listener) {
        if(serviceRequest != null) {
            manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Succeeded to remove service request.");
                    serviceRequest = null;
                    nearbyHostDevices.clear();
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Succeeded to remove service request.");
                    listener.onSuccess();
                }
            });
        }
    }

    /**
     * Connects to the specified hostDevice. If a connection exists prior to calling this method,
     * this method will disconnect said previous connection.
     * @param host The specified hostDevice.
     * @param listener The listener.
     */
    public void connect(final WifiP2pDevice host, final WifiP2pManager.ActionListener listener) {
        if(hostIsNearby(host)) {
            hostPort = getHostRegistrationPort(host);

            disconnect(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Succeeded to disconnect from potential previous connection.");
                    if (host != null) {
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = host.deviceAddress;

                        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Succeeded to connect to hostDevice device.");
                                listener.onSuccess();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "Failed to connect to hostDevice device.");
                                listener.onFailure(reason);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to disconnect from potential previous connection.");
                    listener.onFailure(reason);
                }
            });
        } else {
            Log.d(TAG, "Failed to connect to hostDevice device.");
        }
    }

    /**
     * If a connection to a hostDevice exists, this method will disconnect the device from said hostDevice.
     * @param listener The listener.
     */
    public void disconnect(final WifiP2pManager.ActionListener listener) {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to remove group.");
                listener.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to retrieve group.");
                listener.onFailure(reason);
            }
        });
    }

    /**
     * Sets the {@link WifiP2pManager} DNS response listeners. For internal use only.
     */
    private void setDnsSdResponseListeners() {
        manager.setDnsSdResponseListeners(channel, new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice device) {
            }
        }, new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                if(record != null && record.containsKey(SERVICE_NAME_TAG) && record.get(SERVICE_NAME_TAG).equals(service)) {
                    if (!nearbyHostDevices.keySet().contains(device)) {
                        nearbyHostDevices.put(device, Integer.valueOf(record.get(PORT_TAG)));
                    }
                }
            }
        });
    }

    /**
     * Creates and returns a deep copy of the list of nearby hostDevice {@link WifiP2pDevice}s.
     * @return A deep copy of the list of nearby hostDevice {@link WifiP2pDevice}s.
     */
    public List<WifiP2pDevice> getNearbyHosts() {
        List<WifiP2pDevice> deepNearbyHostsClone = new ArrayList<>();
        for(WifiP2pDevice host : nearbyHostDevices.keySet()) {
            deepNearbyHostsClone.add(new WifiP2pDevice(host));
        }
        return deepNearbyHostsClone;
    }

    /**
     * Creates and returns a copy of the current hostDevice.
     * @return The current hostDevice {@link Device}.
     */
    public Device getHostDevice() {
        return new Device(hostDevice);
    }

    /**
     * Unfortunately, {@link WifiP2pDevice} does not implement {@link Object#hashCode()}; therefore,
     * it is not possible to use {@link Map#containsKey(Object)}.
     * @param host The hostDevice {@link WifiP2pDevice}.
     * @return Whether the given hostDevice {@link WifiP2pDevice} is nearby.
     */
    private boolean hostIsNearby(WifiP2pDevice host) {
        for(WifiP2pDevice device : nearbyHostDevices.keySet()) {
            if(device.equals(host)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unfortunately, {@link WifiP2pDevice} does not implement {@link Object#hashCode()}; therefore,
     * it is not possible to use {@link Map#get(Object)}.
     * @param host The hostDevice {@link WifiP2pDevice}.
     * @return The registration hostPort running on the given hostDevice {@link WifiP2pDevice}.
     */
    private int getHostRegistrationPort(WifiP2pDevice host) {
        for(Map.Entry<WifiP2pDevice, Integer> entry : nearbyHostDevices.entrySet()) {
            if(entry.getKey().equals(host)) {
                return entry.getValue();
            }
        }
        throw new NullPointerException();
    }
}
