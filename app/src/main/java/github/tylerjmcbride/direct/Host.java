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

    private static final int SERVICE_BROADCASTING_INTERVAL = 1000;

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
                if (p2pInfo.groupFormed && networkInfo.isConnected()) {
                    pruneDisconnectedClients();
                } else {
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
            }
        }

        if(!successful) {
            callback.onFailure();
        }
    }

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
                                        serviceBroadcastingThread = new Thread(new Runnable() {
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
                                        });
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

    public void stopService(final WifiP2pManager.ActionListener listener) {
        manager.removeLocalService(channel, info, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to remove local service.");
                if(serviceBroadcastingThread != null) {
                    serviceBroadcastingThread.interrupt();
                }
                registrar.stop();
                objectReceiver.stop();
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

                        // Prune disconnected client information
                        if(containsClient == false) {
                            Log.d(TAG, clientInfo.getMacAddress() + " has disconnected.");
                            clients.remove(clientInfo);
                        }
                    }
                }
            }
        });
    }
}
