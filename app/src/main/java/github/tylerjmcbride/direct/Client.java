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

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.callbacks.DiscoveryCallback;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.registration.ClientRegistrar;
import github.tylerjmcbride.direct.registration.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.registration.listeners.UnregisteredWithServerListener;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.sockets.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.transceivers.DirectBroadcastReceiver;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;

public class Client extends Direct {

    private ClientRegistrar registrar;
    private WifiP2pDnsSdServiceRequest serviceRequest = null;
    private Map<WifiP2pDevice, Integer> nearbyHostDevices = new HashMap<>();

    private WifiP2pDevice hostDevice = null;
    private Integer hostRegistrarPort = null;
    private WifiP2pDeviceInfo hostDeviceInfo = null;
    private ObjectCallback objectCallback;
    private DiscoveryCallback discoveryCallback;

    public Client(Application application, String service) {
        super(application, service);
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
                    Log.d(TAG, "Succeeded to disconnect from host.");
                    hostDevice = null;
                    hostDeviceInfo = null;
                    hostRegistrarPort = null;
                    objectCallback = null;
                    objectReceiver.stop();
                }
            }

            @Override
            protected void peersChanged() {
                pruneLostHosts();
            }

            @Override
            protected void thisDeviceChanged(WifiP2pDevice thisDevice) {
                Client.this.thisDevice = new WifiP2pDevice(thisDevice);
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
     * This method will create a new service request instance and send it to the Wi-Fi P2P framework.
     * If successful, this method will then initiate service discovery. Service discovery is a
     * process that involves scanning for requested services for the purpose of establishing a
     * connection to a peer that supports an available service.
     * @param discoveryCallback The callback when a new service has been discovered.
     * @param resultCallback The callback on the success or failure of the request.
     */
    public void startDiscovery(final DiscoveryCallback discoveryCallback, final ResultCallback resultCallback) {
        if(serviceRequest == null) {
            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
            this.discoveryCallback = discoveryCallback;

            manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Succeeded to add service request.");
                    manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Succeeded to start service discovery.");
                            resultCallback.onSuccess();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "Failed to start service discovery.");
                            resultCallback.onFailure();
                        }
                    });
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to add service request.");
                    resultCallback.onFailure();
                }
            });
        }
    }

    /**
     * This method will remove the service request created in {@link Client#startDiscovery(DiscoveryCallback, ResultCallback)} )},
     * effectively ceasing service discovery. Note that {@link Client#nearbyHostDevices} will be
     * cleared.
     * @param callback The callback.
     */
    public void stopDiscovery(final ResultCallback callback) {
        if(serviceRequest != null) {
            manager.removeServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Succeeded to remove service request.");
                    serviceRequest = null;
                    discoveryCallback = null;
                    nearbyHostDevices.clear();
                    manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Succeeded to stop peer discovery.");
                            callback.onSuccess();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "Failed to stop peer discovery.");
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to remove service request.");
                    callback.onSuccess();
                }
            });
        }
    }

    /**
     * Connects to the specified hostDevice. If a connection exists prior to calling this method,
     * this method will disconnect said previous connection.
     * @param hostDevice The specified hostDevice.
     * @param callback The callback.
     */
    public void connect(final WifiP2pDevice hostDevice, ObjectCallback dataCallback, final ResultCallback callback) {
        if(hostDevice != null && hostIsNearby(hostDevice)) {
            this.hostRegistrarPort = getHostRegistrationPort(hostDevice);
            this.objectCallback = dataCallback;

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = hostDevice.deviceAddress;
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, String.format("Failed to connect to %s.", hostDevice.deviceAddress));
                    callback.onFailure();
                }
            });
        } else {
            Log.d(TAG, "Failed to connect, device is either null or out of range.");
            callback.onFailure();
        }
    }

    /**
     * If a connection to a host exists, this method will disconnect the device from said host.
     * @param callback The callback.
     */
    public void disconnect(final ResultCallback callback) {
        // Must unregister itself with the host before removing the {@link WifiP2pGroup}
        if(hostDevice != null && hostDeviceInfo != null && hostRegistrarPort != null) {
            final String hostMacAddress = hostDevice.deviceAddress;
            final InetSocketAddress hostAddress = new InetSocketAddress(hostDeviceInfo.getIpAddress(), hostRegistrarPort);

            registrar.unregister(hostAddress, new UnregisteredWithServerListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, String.format("Succeeded to unregister with %s.", hostMacAddress));
                    removeGroup(callback);
                }

                @Override
                public void onFailure() {
                    Log.d(TAG, String.format("Failed to unregister with %s.", hostMacAddress));
                    removeGroup(callback);
                }
            });
        } else {
            Log.d(TAG, "Not currently registered.");
            removeGroup(callback);
        }
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
                        discoveryCallback.onDiscovered(new WifiP2pDevice(device));
                    }
                }
            }
        });
    }

    /**
     * Will compare {@link Client#nearbyHostDevices} to the {@link WifiP2pDeviceList} to unsure that
     * all nearby hosts are within range. If any of the existing {@link Client#nearbyHostDevices}
     * are out of range they will be pruned.
     */
    private void pruneLostHosts() {
        manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                for(WifiP2pDevice peer : peers.getDeviceList()) {
                    boolean containsClient = false;

                    for (WifiP2pDevice nearbyHost : nearbyHostDevices.keySet()) {
                        if (peer.deviceAddress.equals(nearbyHost.deviceAddress)) {
                            containsClient = true;
                        }

                        // Prune lost host
                        if(!containsClient) {
                            Log.d(TAG, "Nearby host " + nearbyHost.deviceAddress + " has been lost.");
                            nearbyHostDevices.remove(nearbyHost);
                        }
                    }
                }
            }
        });
    }
}
