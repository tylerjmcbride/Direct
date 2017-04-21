package github.tylerjmcbride.direct.transceivers;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;
import github.tylerjmcbride.direct.sockets.ServerSockets;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.transceivers.runnables.ObjectReceiverRunnable;

public class ObjectReceiver {

    private static final int DEFAULT_RECEIVER_PORT = 0;
    private static final int MAX_SERVER_CONNECTIONS = 25;

    private ServerSocket serverSocket;
    private Handler handler;

    public ObjectReceiver(Handler handler) {
        this.handler = handler;
    }

    /**
     * Starts the data receiver.
     * @param objectCallback The {@link ObjectCallback} to handle received objects.
     * @param initializationListener The {@link ServerSocketInitializationCompleteListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ObjectCallback objectCallback, final ServerSocketInitializationCompleteListener initializationListener) {
        ServerSockets.initializeServerSocket(DEFAULT_RECEIVER_PORT, MAX_SERVER_CONNECTIONS, handler, new ServerSocketInitializationCompleteListener() {
            @Override
            public void onSuccess(ServerSocket serverSocket) {
                Log.d(Direct.TAG, String.format("Succeeded to initialize receiver socket on port %d.", serverSocket.getLocalPort()));
                ObjectReceiver.this.serverSocket = serverSocket;
                new Thread(new ObjectReceiverRunnable(serverSocket, handler, objectCallback)).start();
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
     * Stops the object receiver. Will invoke {@link ServerSocket#close()} which will effectively
     * kill the {@link Thread} running the {@link github.tylerjmcbride.direct.sockets.runnables.ServerSocketRunnable}.
     */
    public void stop() {
        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
                Log.d(Direct.TAG, "Succeeded to stop object receiver.");
            } catch (IOException e) {
                Log.e(Direct.TAG, "Failed to stop object receiver.");
            }
        }
    }
}
