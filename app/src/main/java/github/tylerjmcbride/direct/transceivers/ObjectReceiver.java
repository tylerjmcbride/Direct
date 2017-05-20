package github.tylerjmcbride.direct.transceivers;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.WifiDirect;
import github.tylerjmcbride.direct.sockets.ServerSocketRunnable;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationListener;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;
import github.tylerjmcbride.direct.transceivers.runnables.ObjectReceiverRunnable;

public class ObjectReceiver {

    private static final int DEFAULT_RECEIVER_PORT = 59500;
    private static final int MAX_SERVER_CONNECTIONS = 25;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ServerSocket serverSocket;
    private Handler handler;

    public ObjectReceiver(Handler handler) {
        this.handler = handler;
    }

    /**
     * Starts the data receiver.
     * @param objectCallback The {@link ObjectCallback} to handle received objects.
     * @param listener The {@link ServerSocketInitializationListener} to capture the result of
     *                 the initialization.
     */
    public void start(final ObjectCallback objectCallback, final ServerSocketInitializationListener listener) {
        executor.execute(new ObjectReceiverRunnable(DEFAULT_RECEIVER_PORT, MAX_SERVER_CONNECTIONS, handler, objectCallback, new ServerSocketInitializationListener() {
            @Override
            public void onSuccess(final ServerSocket serverSocket) {
                ObjectReceiver.this.serverSocket = serverSocket;
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
     * Stops the object receiver. Will invoke {@link ServerSocket#close()} which will effectively
     * kill the {@link Thread} running the {@link ServerSocketRunnable}.
     */
    public void stop() {
        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
                Log.d(WifiDirect.TAG, "Succeeded to stop object receiver.");
            } catch (IOException e) {
                Log.e(WifiDirect.TAG, "Failed to stop object receiver.");
            }
        }
    }
}
