package github.tylerjmcbride.direct.registration;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Client;
import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.registration.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.registration.listeners.UnregisteredWithServerListener;
import github.tylerjmcbride.direct.registration.model.Adieu;
import github.tylerjmcbride.direct.registration.model.Handshake;
import github.tylerjmcbride.direct.sockets.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.sockets.SocketRunnable;

public class ClientRegistrar {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Client client;
    private Handler handler;

    public ClientRegistrar(Client client, Handler handler) {
        this.client = client;
        this.handler = handler;
    }

    public void register(InetSocketAddress address, final RegisteredWithServerListener registeredWithServerListener) {
        executor.submit(new SocketRunnable(address, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(final Socket hostSocket) {
                try {
                    // Send details about the client device
                    WifiP2pDeviceInfo info = client.getThisDeviceInfo();
                    ObjectOutputStream outputStream = new ObjectOutputStream(hostSocket.getOutputStream());
                    outputStream.writeObject(new Handshake(info.getMacAddress(), info.getPort()));
                    outputStream.flush();

                    // Retrieve details about the host device
                    ObjectInputStream inputStream = new ObjectInputStream(hostSocket.getInputStream());
                    Handshake handshake = (Handshake) inputStream.readObject();

                    // Notify framework
                    final WifiP2pDeviceInfo hostInfo = new WifiP2pDeviceInfo(handshake.getMacAddress(), hostSocket.getInetAddress(), handshake.getPort());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            registeredWithServerListener.onSuccess(hostInfo);
                        }
                    });

                    outputStream.close();
                    inputStream.close();
                } catch (ClassNotFoundException | ClassCastException | IOException ex) {
                    Log.e(Direct.TAG, "Failed to register with server.");
                    Log.e(Direct.TAG, ex.getMessage());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            registeredWithServerListener.onFailure();
                        }
                    });
                } finally {
                    try {
                        hostSocket.close();
                    } catch (Exception ex) {
                        Log.e(Direct.TAG, "Failed to close registration socket.");
                    }
                }
            }

            @Override
            public void onFailure() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        registeredWithServerListener.onFailure();
                    }
                });
            }
        }));
    }

    public void unregister(InetSocketAddress address, final UnregisteredWithServerListener unregisteredWithServerListener) {
        executor.submit(new SocketRunnable(address, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(final Socket hostSocket) {
                try {
                    // Send the unregister request to the host
                    WifiP2pDeviceInfo info = client.getThisDeviceInfo();
                    ObjectOutputStream outputStream = new ObjectOutputStream(hostSocket.getOutputStream());
                    outputStream.writeObject(new Adieu(info.getMacAddress(), info.getPort()));
                    outputStream.flush();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            unregisteredWithServerListener.onSuccess();
                        }
                    });

                    outputStream.close();
                } catch (IOException ex) {
                    Log.e(Direct.TAG, "Failed to unregister with server.");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            unregisteredWithServerListener.onFailure();
                        }
                    });
                } finally {
                    try {
                        hostSocket.close();
                    } catch (Exception ex) {
                        Log.e(Direct.TAG, "Failed to close registration socket.");
                    }
                }
            }

            @Override
            public void onFailure() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        unregisteredWithServerListener.onFailure();
                    }
                });
            }
        }));
    }
}
