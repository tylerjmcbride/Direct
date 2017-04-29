package github.tylerjmcbride.direct.registration;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.Host;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.registration.listeners.HandshakeListener;
import github.tylerjmcbride.direct.registration.model.Adieu;
import github.tylerjmcbride.direct.registration.model.Handshake;
import github.tylerjmcbride.direct.sockets.ServerSockets;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.sockets.runnables.ServerSocketRunnable;

/**
 * A {@link HostRegistrar} is in charge of handling the registration of client {@link WifiP2pDevice}s.
 */
public class HostRegistrar {

    private static final int DEFAULT_REGISTRATION_PORT = 59250;
    private static final int MAX_SERVER_CONNECTIONS = 25;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Host host;
    private Handler handler;
    private ServerSocket serverSocket;
    private HandshakeListener handshakeListener;

    public HostRegistrar(Host host, Handler handler, HandshakeListener handshakeListener) {
        this.host = host;
        this.handler = handler;
        this.handshakeListener = handshakeListener;
    }

    /**
     * Starts the registration process.
     * @param initializationCompleteListener The {@link ServerSocketInitializationCompleteListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ServerSocketInitializationCompleteListener initializationCompleteListener) {
        ServerSockets.initializeServerSocket(DEFAULT_REGISTRATION_PORT, MAX_SERVER_CONNECTIONS, handler, new ServerSocketInitializationCompleteListener() {
            @Override
            public void onSuccess(ServerSocket serverSocket) {
                Log.d(Direct.TAG, String.format("Succeeded to initialize registration socket on port %d.", serverSocket.getLocalPort()));
                HostRegistrar.this.serverSocket = serverSocket;

                executor.submit(new ServerSocketRunnable(serverSocket) {
                    @Override
                    public void onConnected(final Socket clientSocket) {
                        try {
                            // Retrieve details about the client device
                            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
                            Object object = inputStream.readObject();

                            if(object instanceof Handshake) {
                                Handshake handshake = (Handshake) object;
                                final WifiP2pDeviceInfo clientInfo = new WifiP2pDeviceInfo(handshake.getMacAddress(), clientSocket.getInetAddress(), handshake.getPort());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        handshakeListener.onHandshake(clientInfo);
                                    }
                                });

                                // Send details about the host device
                                WifiP2pDeviceInfo info = host.getThisDeviceInfo();
                                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                                outputStream.writeObject(new Handshake(info.getMacAddress(), info.getPort()));
                                outputStream.flush();
                                outputStream.close();
                            } else if(object instanceof Adieu) {
                                Adieu adieu = (Adieu) object;
                                final WifiP2pDeviceInfo clientInfo = new WifiP2pDeviceInfo(adieu.getMacAddress(), clientSocket.getInetAddress(), adieu.getPort());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        handshakeListener.onAdieu(clientInfo);
                                    }
                                });
                            }

                            inputStream.close();
                        } catch (ClassNotFoundException | IOException ex) {
                            Log.e(Direct.TAG, "Failed to register client.");
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (Exception ex) {
                                Log.e(Direct.TAG, "Failed to close client socket.");
                            }
                        }
                    }
                });

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
     * Stops the registration process. Will invoke {@link ServerSocket#close()} which will effectively
     * kill the {@link Thread} running the {@link github.tylerjmcbride.direct.sockets.runnables.ServerSocketRunnable}.
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
