package github.tylerjmcbride.direct.registration;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.DataListener;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;

public class DataHandler {

    private static final int DEFAULT_REGISTRATION_PORT = 37500;
    private static final int MAX_SERVER_CONNECTIONS = 25;
    private static final int BUFFER_SIZE = 65536;

    private ServerSocket serverSocket;
    private DataListener listener;
    private Handler handler;


    public DataHandler(Handler handler) {
        this.handler = handler;
        this.listener = null;
    }

    /**
     * Starts the data receiver.
     * @param listener The {@link ServerSocketInitializationCompleteListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ServerSocketInitializationCompleteListener listener) {
        ServerSockets.initializeServerSocket(DEFAULT_REGISTRATION_PORT, MAX_SERVER_CONNECTIONS, BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
            @Override
            public void onSuccess(ServerSocket socket) {
                Log.d(Direct.TAG, String.format("Succeeded to initialize data receiver socket on port %d.", socket.getLocalPort()));
                serverSocket = socket;
                listener.onSuccess(socket);
            }

            @Override
            public void onFailure() {
                Log.e(Direct.TAG, "Failed to initialize data receiver socket.");
                listener.onFailure();
            }
        });
    }

    /**
     * Stops the data receiver.
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

    public void setListener(DataListener listener) {
        this.listener = listener;
    }
}
