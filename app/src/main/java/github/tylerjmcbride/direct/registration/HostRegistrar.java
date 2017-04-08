package github.tylerjmcbride.direct.registration;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.HandshakeListener;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.model.data.Handshake;
import github.tylerjmcbride.direct.utilities.runnables.ServerSocketRunnable;
import github.tylerjmcbride.direct.utilities.ServerSockets;

/**
 * A {@link HostRegistrar} is in charge of handling the registration of client {@link WifiP2pDevice}s.
 */
public class HostRegistrar extends Registrar {

    private static final int DEFAULT_REGISTRATION_PORT = 37500;
    private static final int MAX_SERVER_CONNECTIONS = 25;

    private ServerSocket serverSocket;
    private HandshakeListener handshakeListener;

    public HostRegistrar(Direct direct, Handler handler, HandshakeListener handshakeListener) {
        super(direct, handler);
        this.handshakeListener = handshakeListener;
    }

    /**
     * Starts the registration process.
     * @param initializationCompleteListener The {@link ServerSocketInitializationCompleteListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ServerSocketInitializationCompleteListener initializationCompleteListener) {
        ServerSockets.initializeServerSocket(DEFAULT_REGISTRATION_PORT, MAX_SERVER_CONNECTIONS, BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
            @Override
            public void onSuccess(ServerSocket serverSocket) {
                Log.d(Direct.TAG, String.format("Succeeded to initialize registration socket on port %d.", serverSocket.getLocalPort()));
                HostRegistrar.this.serverSocket = serverSocket;

                new Thread(new ServerSocketRunnable(serverSocket, Executors.newFixedThreadPool(5)) {
                    @Override
                    public void onConnected(final Socket clientSocket) {
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Retrieve details about the client device
                                    ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
                                    Handshake handshake = (Handshake) inputStream.readObject();

                                    // Notify framework
                                    final WifiP2pDeviceInfo clientInfo = new WifiP2pDeviceInfo(handshake.getMacAddress(), clientSocket.getInetAddress(), handshake.getPort());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            handshakeListener.onHandshake(clientInfo);
                                        }
                                    });

                                    // Send details about the host device
                                    WifiP2pDeviceInfo info = direct.getThisDeviceInfo();
                                    ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                                    outputStream.writeObject(new Handshake(info.getMacAddress(), info.getPort()));

                                    outputStream.close();
                                    inputStream.close();
                                } catch (ClassNotFoundException | ClassCastException | IOException ex) {
                                    Log.e(Direct.TAG, "Failed to register client.");
                                    Log.e(Direct.TAG, ex.getMessage());
                                } finally {
                                    try {
                                        clientSocket.close();
                                    } catch (Exception ex) {
                                        Log.e(Direct.TAG, "Failed to close client socket.");
                                    }
                                }
                            }
                        });
                    }
                }).start();

                initializationCompleteListener.onSuccess(serverSocket);
            }

            @Override
            public void onFailure() {
                Log.e(Direct.TAG, "Failed to initialize registration socket.");
                initializationCompleteListener.onFailure();
            }
        });
    }

    /**
     * Stops the registration process.
     */
    public void stop() {
        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
                Log.d(Direct.TAG, "Succeeded to stop registrar.");
            } catch (IOException e) {
                Log.e(Direct.TAG, "Failed to stop registrar.");
            }
        }
    }
}
