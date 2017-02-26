package github.tylerjmcbride.direct;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.listeners.ClientRegisteredListener;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.Device;
import github.tylerjmcbride.direct.receivers.DirectBroadcastReceiver;
import github.tylerjmcbride.direct.registration.HostRegistrar;

public class Host extends Direct {

    private static final int SERVICE_BROADCASTING_INTERVAL = 1000;

    private HostRegistrar registrar;
    private WifiP2pDnsSdServiceInfo info;
    private Map<String, String> record = new HashMap<>();
    private Thread serviceBroadcastingThread;

    private List<Device> clients = Collections.synchronizedList(new ArrayList<Device>());

    public Host(Application application, final String service, final int serverPort, final String instance) {
        super(application, service, serverPort, instance);
        receiver = new DirectBroadcastReceiver(manager, channel) {
            @Override
            protected void connectionChanged(Context context, Intent intent, NetworkInfo info) {
                if (info.isConnected()) {
                    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            //TODO probably need something here
                        }
                    });
                }
            }

            @Override
            protected void peersChanged(Context context, Intent intent) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        nearbyPeers = peers;
                    }
                });
            }

            @Override
            protected void stateChanged(Context context, Intent intent, boolean wifiEnabled) {
                //TODO Add some useful functionality here
            }

            @Override
            protected void thisDeviceChanged(Context context, Intent intent, WifiP2pDevice device) {
                thisDevice = new Device(device.deviceName, device.deviceAddress, wifiManager.getConnectionInfo().getIpAddress(), 5445);
            }
        };
        application.getApplicationContext().registerReceiver(receiver, intentFilter);
        registrar = new HostRegistrar(this);

        record.put(SERVICE_NAME_TAG, service);
        info = WifiP2pDnsSdServiceInfo.newInstance(instance, service.concat("._tcp"), record);
    }

    public void startService(final WifiP2pManager.ActionListener listener) {
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to clear local service.");
                registrar.stop();
                clients.clear();

                registrar.start(new ServerSocketInitializationCompleteListener() {
                    @Override
                    public void onSuccess(ServerSocket socket) {
                        Log.d(TAG, "Succeeded to start registrar.");

                        // Reinitialize the service information to reflect the new registration port
                        record.put(REGISTRATION_PORT_TAG, Integer.toString(socket.getLocalPort()));
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
                }, new ClientRegisteredListener() {
                    @Override
                    public void onClientRegistered(Device device) {
                        clients.add(device);
                    }

                    @Override
                    public void onClientUnregistered(Device device) {
                        clients.remove(device);
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
                clients.clear();
                listener.onSuccess();
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
    public List<Device> getClients() {
        List<Device> deepClientsClone = new ArrayList<>();
        for(Device device : clients) {
            deepClientsClone.add(new Device(device));
        }
        return deepClientsClone;
    }
}
