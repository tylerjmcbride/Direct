package github.tylerjmcbride.direct.registration;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.WifiDirect;
import github.tylerjmcbride.direct.WifiDirectHost;
import github.tylerjmcbride.direct.registration.listeners.HandshakeListener;
import github.tylerjmcbride.direct.registration.runnables.HostRegistrarRunnable;
import github.tylerjmcbride.direct.sockets.ServerSocketRunnable;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationListener;

/**
 * A {@link HostRegistrar} is in charge of handling the registration of client {@link WifiP2pDevice}s.
 */
public class HostRegistrar {

    private static final int DEFAULT_REGISTRATION_PORT = 59250;
    private static final int MAX_SERVER_CONNECTIONS = 25;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private WifiDirectHost host;
    private Handler handler;
    private ServerSocket serverSocket;
    private HandshakeListener handshakeListener;

    public HostRegistrar(WifiDirectHost host, Handler handler, HandshakeListener handshakeListener) {
        this.host = host;
        this.handler = handler;
        this.handshakeListener = handshakeListener;
    }

    /**
     * Starts the registration process.
     * @param listener The {@link ServerSocketInitializationListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ServerSocketInitializationListener listener) {
        executor.execute(new HostRegistrarRunnable(DEFAULT_REGISTRATION_PORT, MAX_SERVER_CONNECTIONS, handler, host, handshakeListener, new ServerSocketInitializationListener() {
            @Override
            public void onSuccess(final ServerSocket serverSocket) {
                HostRegistrar.this.serverSocket = serverSocket;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess(serverSocket);
                    }
                });
            }

            @Override
            public void onFailure() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFailure();
                    }
                });
            }
        }));
    }

    /**
     * Stops the registration process. Will invoke {@link ServerSocket#close()} which will effectively
     * kill the {@link Thread} running the {@link ServerSocketRunnable}.
     */
    public void stop() {
        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
                Log.d(WifiDirect.TAG, "Succeeded to stop registrar.");
            } catch (IOException e) {
                Log.e(WifiDirect.TAG, "Failed to stop registrar.");
            }
        }
    }
}
