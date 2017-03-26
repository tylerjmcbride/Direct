package github.tylerjmcbride.direct.registration;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.HandshakeListener;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.model.data.HandshakeData;
import github.tylerjmcbride.direct.registration.runnables.ServerSocketRunnable;

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
                                    DataInputStream from = new DataInputStream(clientSocket.getInputStream());
                                    DataOutputStream to = new DataOutputStream(clientSocket.getOutputStream());

                                    // Client sends details about their device
                                    final HandshakeData data = LoganSquare.parse(from.readUTF(), HandshakeData.class);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            handshakeListener.onHandshake(new WifiP2pDeviceInfo(data.getMacAddress(), clientSocket.getInetAddress(), data.getPort()));
                                        }
                                    });

                                    // Reply by sending details about the host device
                                    WifiP2pDeviceInfo info = direct.getThisDeviceInfo();
                                    to.writeUTF(LoganSquare.serialize(new HandshakeData(info.getMacAddress(), info.getPort())));
                                    to.flush();

                                    from.close();
                                    to.close();

                                } catch (Exception ex) {
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
