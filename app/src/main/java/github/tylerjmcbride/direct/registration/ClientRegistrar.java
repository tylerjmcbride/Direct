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

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.model.data.Handshake;
import github.tylerjmcbride.direct.utilities.runnables.SocketConnectionRunnable;

public class ClientRegistrar extends Registrar {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ClientRegistrar(Direct direct, Handler handler) {
        super(direct, handler);
    }

    public void register(InetSocketAddress address, final RegisteredWithServerListener registeredWithServerListener) {
        executor.submit(new SocketConnectionRunnable(address, BUFFER_SIZE, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(final Socket hostSocket) {
                try {
                    // Send details about the client device
                    WifiP2pDeviceInfo info = direct.getThisDeviceInfo();
                    ObjectOutputStream outputStream = new ObjectOutputStream(hostSocket.getOutputStream());
                    outputStream.writeObject(new Handshake(info.getMacAddress(), info.getPort()));

                    // Retrieve details about the host device
                    ObjectInputStream  inputStream = new ObjectInputStream(hostSocket.getInputStream());
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
}
