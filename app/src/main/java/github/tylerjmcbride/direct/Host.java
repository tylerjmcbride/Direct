package github.tylerjmcbride.direct;

import android.app.Application;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.listeners.HandshakeListener;
import github.tylerjmcbride.direct.listeners.ObjectCallback;
import github.tylerjmcbride.direct.listeners.ResultCallback;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.receivers.DirectBroadcastReceiver;
import github.tylerjmcbride.direct.registration.HostRegistrar;

public class Host extends Direct {

    private HostRegistrar registrar;
    private WifiP2pDnsSdServiceInfo info;
    private Map<String, String> record = new HashMap<>();
    private Thread serviceBroadcastingThread;

    private Map<WifiP2pDeviceInfo, WifiP2pDevice> clients = new HashMap<>();

    public Host(Application application, final String service, final String instance) {
        super(application, service, instance);
        receiver = new DirectBroadcastReceiver(manager, channel) {
            @Override
            protected void connectionChanged(WifiP2pInfo p2pInfo, NetworkInfo networkInfo, WifiP2pGroup p2pGroup) {
                // No need to check {@link WifiP2pInfo#isGroupOwner} as this device is dedicated to hosting the service
                if (p2pInfo.groupFormed && networkInfo.isConnected()) {
                    pruneDisconnectedClients();
                } else {
                    registrar.stop();
                    objectReceiver.stop();
                    clients.clear();
                }
            }

            @Override
            protected void peersChanged() {
                pruneDisconnectedClients();
            }

            @Override
            protected void thisDeviceChanged(WifiP2pDevice thisDevice) {
                thisDevice = new WifiP2pDevice(thisDevice);
                thisDeviceInfo.setMacAddress(thisDevice.deviceAddress);
            }
        };
        application.getApplicationContext().registerReceiver(receiver, intentFilter);

        registrar = new HostRegistrar(this, handler, new HandshakeListener() {
            @Override
            public void onHandshake(final WifiP2pDeviceInfo clientInfo) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        WifiP2pDevice clientDevice = peers.get(clientInfo.getMacAddress());

                        if(clientDevice != null) {
                            Log.d(TAG, String.format("Succeeded to register client %s.", clientInfo.getMacAddress()));
                            clients.put(clientInfo, clientDevice);
                        } else {
                            Log.d(TAG, String.format("Failed to register client %s.", clientInfo.getMacAddress()));
                        }
                    }
                });
            }

            @Override
            public void onAdieu(WifiP2pDeviceInfo clientInfo) {
                Log.d(TAG, clientInfo.getMacAddress() + " has unregistered.");
                clients.remove(clientInfo);
            }
        });

        record.put(SERVICE_NAME_TAG, service);
        info = WifiP2pDnsSdServiceInfo.newInstance(instance, service.concat("._tcp"), record);
    }

    /**
     * Sends the respective client the given serializable object.
     * @param client The client to receive the object.
     * @param object The object to send to the respective client.
     * @param callback The callback to capture the result.
     */
    public void send(WifiP2pDevice client, Serializable object, final ResultCallback callback) {
        boolean successful = false;

        for(WifiP2pDeviceInfo clientInfo : clients.keySet()) {
            if(client != null && client.deviceAddress.equals(clientInfo.getMacAddress())) {
                successful = true;

                objectTransmitter.send(object, new InetSocketAddress(clientInfo.getIpAddress(), clientInfo.getPort()), new SocketInitializationCompleteListener() {
                    @Override
                    public void onSuccess(Socket socket) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure() {
                        callback.onFailure();
                    }
                });

                break;
            }
        }

        if(!successful) {
            callback.onFailure();
        }
    }

    /**
     * Registers the local service for service discovery effectively starting the service; however,
     * this is only a request to add said local service, the service will not officially be added
     * until the framework is notified.
     * @param listener The listener.
     */
    public void startService(final ObjectCallback dataCallback, final WifiP2pManager.ActionListener listener) {
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to clear local service.");
                objectReceiver.start(dataCallback, new ServerSocketInitializationCompleteListener() {
                    @Override
                    public void onSuccess(ServerSocket serverSocket) {
                        Log.d(TAG, String.format("Succeeded to start object receiver on port %d.", serverSocket.getLocalPort()));
                        thisDeviceInfo.setPort(serverSocket.getLocalPort());

                        registrar.start(new ServerSocketInitializationCompleteListener() {
                            @Override
                            public void onSuccess(ServerSocket serverSocket) {
                                Log.d(TAG, String.format("Succeeded to start registrar on port %d.", serverSocket.getLocalPort()));

                                // Reinitialize the service information to reflect the new registration port
                                record.put(REGISTRAR_PORT_TAG, Integer.toString(serverSocket.getLocalPort()));
                                info = WifiP2pDnsSdServiceInfo.newInstance(instance, service.concat("._tcp"), record);

                                manager.addLocalService(channel, info, new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Succeeded to add local service.");
                                        serviceBroadcastingThread = new Thread(new ServiceBroadcastingRunnable());
                                        serviceBroadcastingThread.start();
                                        listener.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(int reason) {
                                        Log.d(TAG, "Failed to add local service.");
                                        listener.onFailure(reason);
                                    }
                                });
                            }

                            @Override
                            public void onFailure() {
                                Log.d(TAG, "Failed to start registrar.");
                                listener.onFailure(0);
                            }
                        });
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to start object receiver.");
                        listener.onFailure(0);
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to clear local services.");
                listener.onFailure(reason);
            }
        });
    }

    /**
     * Removes the registered local service effectively stopping the service; however, this is only
     * a request to remove a local service, the service will not officially be removed until the
     * framework is notified.
     * @param listener The listener.
     */
    public void stopService(final WifiP2pManager.ActionListener listener) {
        manager.removeLocalService(channel, info, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to remove local service.");
                if(serviceBroadcastingThread != null) {
                    serviceBroadcastingThread.interrupt();
                }
                removeGroup(listener);
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to remove local service.");
                listener.onFailure(reason);
            }
        });
    }

    /**
     * Creates and returns a deep copy of the list of client {@link WifiP2pDevice}s.
     * @return A deep copy of the list of client {@link WifiP2pDevice}s.
     */
    public List<WifiP2pDevice> getRegisteredClients() {
        List<WifiP2pDevice> deepClientsClone = new ArrayList<>();
        for(WifiP2pDevice device : clients.values()) {
            deepClientsClone.add(new WifiP2pDevice(device));
        }
        return deepClientsClone;
    }

    /**
     * Will compare {@link Host#clients} to the {@link WifiP2pDeviceList} to unsure that all
     * registered clients are within range. If any of the existing {@link Host#clients} are out of
     * range they will be pruned.
     */
    private void pruneDisconnectedClients() {
        manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                for(WifiP2pDevice peer : peers.getDeviceList()) {
                    boolean containsClient = false;

                    for (WifiP2pDeviceInfo clientInfo : clients.keySet()) {
                        if (peer.deviceAddress.equals(clientInfo.getMacAddress())) {
                            containsClient = true;
                        }

                        // Prune disconnected client
                        if(containsClient == false) {
                            Log.d(TAG, clientInfo.getMacAddress() + " has disconnected.");
                            clients.remove(clientInfo);
                        }
                    }
                }
            }
        });
    }

    /**
     * Will continually broadcast the service. The {@link ServiceBroadcastingRunnable} will run on
     * its respective {@link Thread} until {@link Thread#interrupt()} is called.
     */
    class ServiceBroadcastingRunnable implements Runnable {

        private static final int SERVICE_BROADCASTING_INTERVAL = 1000;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(SERVICE_BROADCASTING_INTERVAL);
                    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int error) {
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
