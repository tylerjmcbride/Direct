package github.tylerjmcbride.direct;

import android.app.Application;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.listeners.ObjectCallback;
import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.listeners.ResultCallback;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.listeners.UnregisteredWithServerListener;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.receivers.DirectBroadcastReceiver;
import github.tylerjmcbride.direct.registration.ClientRegistrar;

public class Client extends Direct {

    private ClientRegistrar registrar;
    private WifiP2pDnsSdServiceRequest serviceRequest = null;
    private Map<WifiP2pDevice, Integer> nearbyHostDevices = new HashMap<>();

    private WifiP2pDevice hostDevice = null;
    private Integer hostRegistrarPort = null;
    private WifiP2pDeviceInfo hostDeviceInfo = null;
    private ObjectCallback objectCallback;

    public Client(Application application, String service, String instance) {
        super(application, service, instance);
        receiver = new DirectBroadcastReceiver(manager, channel) {
            @Override
            protected void connectionChanged(WifiP2pInfo p2pInfo, NetworkInfo networkInfo, WifiP2pGroup p2pGroup) {
                if (hostDevice == null && hostRegistrarPort != null && p2pInfo.groupFormed && networkInfo.isConnected()) {
                    Log.d(TAG, "Succeeded to connect to host.");
                    hostDevice = p2pGroup.getOwner();
                    final InetSocketAddress hostAddress = new InetSocketAddress(p2pInfo.groupOwnerAddress.getHostAddress(), hostRegistrarPort);

                    objectReceiver.start(objectCallback, new ServerSocketInitializationCompleteListener() {
                        @Override
                        public void onSuccess(ServerSocket serverSocket) {
                            Log.d(TAG, String.format("Succeeded to start object receiver on port %d.", serverSocket.getLocalPort()));
                            thisDeviceInfo.setPort(serverSocket.getLocalPort());

                            registrar.register(hostAddress, new RegisteredWithServerListener() {
                                @Override
                                public void onSuccess(WifiP2pDeviceInfo info) {
                                    Log.d(TAG, "Succeeded to register with " + info.getMacAddress() + ".");
                                    hostDeviceInfo = info;
                                }

                                @Override
                                public void onFailure() {
                                    Log.d(TAG, "Failed to register with host.");
                                }
                            });
                        }

                        @Override
                        public void onFailure() {
                            Log.d(TAG, "Failed to start data receiver.");
                        }
                    });
                } else {
                    // Unregister with the host
                    if(hostDevice != null && hostDeviceInfo != null && hostRegistrarPort != null) {
                        final String hostMacAddress = hostDevice.deviceAddress;
                        final InetSocketAddress hostAddress = new InetSocketAddress(hostDeviceInfo.getIpAddress(), hostRegistrarPort);

                        registrar.unregister(hostAddress, new UnregisteredWithServerListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, String.format("Succeeded to unregister with %s.", hostMacAddress));
                            }

                            @Override
                            public void onFailure() {
                                Log.d(TAG, String.format("Failed to unregister with %s.", hostMacAddress));
                            }
                        });
                    }

                    hostDevice = null;
                    hostDeviceInfo = null;
                    hostRegistrarPort = null;
                    objectCallback = null;
                    objectReceiver.stop();
                }
            }

            @Override
            protected void thisDeviceChanged(WifiP2pDevice thisDevice) {
                thisDevice = new WifiP2pDevice(thisDevice);
                thisDeviceInfo.setMacAddress(thisDevice.deviceAddress);
            }
        };
        application.getApplicationContext().registerReceiver(receiver, intentFilter);
        registrar = new ClientRegistrar(this, handler);
        setDnsSdResponseListeners();
    }

    /**
     * Sends the host the given serializable object.
     * @param object The object to send to the host.
     * @param callback The callback to capture the result.
     */
    public void send(Serializable object, final ResultCallback callback) {
        if(hostDevice != null && hostDeviceInfo != null) {
            objectTransmitter.send(object, new InetSocketAddress(hostDeviceInfo.getIpAddress(), hostDeviceInfo.getPort()), new SocketInitializationCompleteListener() {
                @Override
                public void onSuccess(Socket socket) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });
        } else {
            callback.onFailure();
        }
    }

    /**
     * Starts the discovery of services.
     * @param listener The listener.
     */
    public void startDiscovery(final WifiP2pManager.ActionListener listener) {
        if(serviceRequest == null) {
            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

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
    }

    /**
     * Ends the discovery of services.
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
                    manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Succeeded to stop peer discovery.");
                            listener.onSuccess();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "Failed to stop peer discovery.");
                            listener.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to remove service request.");
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
    public void connect(final WifiP2pDevice host, ObjectCallback dataCallback, final WifiP2pManager.ActionListener listener) {
        if(host != null && hostIsNearby(host)) {
            this.hostRegistrarPort = getHostRegistrationPort(host);
            this.objectCallback = dataCallback;

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = host.deviceAddress;
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to connect to " + host.deviceAddress + ". Reason: " + reason + ".");
                    listener.onFailure(reason);
                }
            });
        } else {
            Log.d(TAG, "Failed to connect to device.");
            listener.onFailure(0);
        }
    }

    /**
     * If a connection to a hostDevice exists, this method will disconnect the device from said hostDevice.
     * @param listener The listener.
     */
    public void disconnect(final WifiP2pManager.ActionListener listener) {
        removeGroup(new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                listener.onSuccess();
            }

            @Override
            public void onFailure(int reason) {
                listener.onFailure(reason);
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
     * @return The current hostDevice {@link WifiP2pDevice}.
     */
    public WifiP2pDevice getHostDevice() {
        return new WifiP2pDevice(hostDevice);
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

    /**
     * Sets the {@link WifiP2pManager} DNS response listeners. For internal use only.
     */
    private void setDnsSdResponseListeners() {
        manager.setDnsSdResponseListeners(channel, new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice device) {
                // Nothing to be done here
            }
        }, new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                if(record != null && record.containsKey(SERVICE_NAME_TAG) && record.get(SERVICE_NAME_TAG).equals(service)) {
                    if (!nearbyHostDevices.keySet().contains(device)) {
                        Log.d(TAG, "Succeeded to retrieve " + device.deviceAddress + " txt record.");
                        nearbyHostDevices.put(device, Integer.valueOf(record.get(REGISTRAR_PORT_TAG)));
                    }
                }
            }
        });
    }
}
