package github.tylerjmcbride.direct.transceivers;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.ObjectCallback;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.utilities.ServerSockets;
import github.tylerjmcbride.direct.transceivers.runnables.ObjectReceiverRunnable;

public class ObjectReceiver {

    private static final int DEFAULT_RECEIVER_PORT = 0;
    private static final int MAX_SERVER_CONNECTIONS = 25;
    private static final int BUFFER_SIZE = 65536;

    private ServerSocket serverSocket;
    private Handler handler;

    public ObjectReceiver(Handler handler) {
        this.handler = handler;
    }

    /**
     * Starts the data receiver.
     * @param dataCallback The {@link ObjectCallback} to handle received data.
     * @param initializationListener The {@link ServerSocketInitializationCompleteListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ObjectCallback dataCallback, final ServerSocketInitializationCompleteListener initializationListener) {
        ServerSockets.initializeServerSocket(DEFAULT_RECEIVER_PORT, MAX_SERVER_CONNECTIONS, BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
            @Override
            public void onSuccess(ServerSocket serverSocket) {
                Log.d(Direct.TAG, String.format("Succeeded to initialize receiver socket on port %d.", serverSocket.getLocalPort()));
                ObjectReceiver.this.serverSocket = serverSocket;
                new Thread(new ObjectReceiverRunnable(serverSocket, handler, dataCallback)).start();
                initializationListener.onSuccess(serverSocket);
            }

            @Override
            public void onFailure() {
                Log.e(Direct.TAG, "Failed to initialize receiver socket.");
                initializationListener.onFailure();
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
                Log.d(Direct.TAG, "Succeeded to stop receiver.");
            } catch (IOException e) {
                Log.e(Direct.TAG, "Failed to stop receiver.");
            }
        }
    }
}
