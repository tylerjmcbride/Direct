package github.tylerjmcbride.direct.sockets;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.WifiDirect;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationListener;

/**
 * The {@link ServerSocketRunnable} will run on its respective {@link Thread} until
 * {@link ServerSocket#close()} is called, or alternatively {@link Thread#interrupt()} is called.
 * To prevent a {@link android.os.NetworkOnMainThreadException}, this runnable should not be run on
 * the main thread.
 */
public abstract class ServerSocketRunnable extends AbstractSocketRunnable implements Runnable {

    private ExecutorService executor;

    private int port;
    private int maxServerConnections;
    private ServerSocketInitializationListener listener;

    protected Handler handler;

    public ServerSocketRunnable(int port, int maxServerConnections, Handler handler, ServerSocketInitializationListener listener) {
        this.executor = Executors.newFixedThreadPool(5);
        this.port = port;
        this.maxServerConnections = maxServerConnections;
        this.handler = handler;
        this.listener = listener;
    }

    /**
     * Will accept incoming connections and process said connections accordingly.
     *
     * @param serverSocket The respective server socket.
     */
    private void acceptConnections(ServerSocket serverSocket) {
        try {
            // Will run indefinitely unless the current thread is interrupted
            while (!Thread.currentThread().isInterrupted()) {
                final Socket socket = serverSocket.accept();
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        onConnected(socket);
                    }
                });
            }

            // Current thread has been interrupted, clean up registration socket
            Log.d(WifiDirect.TAG, String.format("Succeeded to close socket running on port %d.", serverSocket.getLocalPort()));
            serverSocket.close();
        } catch (SocketException ex) {
            // This exception is used to interrupt the current thread
            Log.d(WifiDirect.TAG, String.format("Succeeded to close socket running on port %d.", serverSocket.getLocalPort()));
        } catch (IOException ex) {
            Log.e(WifiDirect.TAG, String.format("Unexpected exception thrown by socket %d.", serverSocket.getLocalPort()));
        }

        // Clean up executor service
        executor.shutdownNow();
    }

    @Override
    public void run() {
        // Attempt to initialize the server socket on the given port
        initialize(port, maxServerConnections, DEFAULT_BUFFER_SIZE, new ServerSocketInitializationListener() {
            @Override
            public void onSuccess(final ServerSocket serverSocket) {
                Log.d(WifiDirect.TAG, String.format("Succeeded to initialize socket on port %d.", serverSocket.getLocalPort()));
                listener.onSuccess(serverSocket);
                acceptConnections(serverSocket);
            }

            @Override
            public void onFailure() {
                // Attempt to initialize server socket on a random port
                initialize(0, maxServerConnections, DEFAULT_BUFFER_SIZE, new ServerSocketInitializationListener() {
                    @Override
                    public void onSuccess(final ServerSocket serverSocket) {
                        Log.d(WifiDirect.TAG, String.format("Succeeded to initialize socket on port %d.", serverSocket.getLocalPort()));
                        listener.onSuccess(serverSocket);
                        acceptConnections(serverSocket);
                    }

                    @Override
                    public void onFailure() {
                        Log.d(WifiDirect.TAG, "Failed to initialize socket");
                        listener.onFailure();
                    }
                });
            }
        });
    }

    /**
     * Attempts to initialize the {@link ServerSocket}.
     * @param port The port for the {@link ServerSocket} to listen on.
     * @param maxServerConnections The maximum number of connections allowed at any given time.
     * @param bufferSize The size of the buffer.
     * @param listener The {@link ResultCallback} to capture the result of the given method call.
     */
    private static void initialize(int port, int maxServerConnections, int bufferSize, ServerSocketInitializationListener listener) {
        try {
            ServerSocket socket = new ServerSocket(port, maxServerConnections);
            socket.setReuseAddress(true);
            socket.setReceiveBufferSize(bufferSize);
            listener.onSuccess(socket);
        } catch (IOException ex) {
            Log.d(WifiDirect.TAG, String.format("The port %d is unavailable.", port));
            listener.onFailure();
        }
    }


    /**
     * Will be invoked when a connection is established to the {@link ServerSocket}.
     * @param socket The {@link Socket} that has established connection.
     */
    public abstract void onConnected(Socket socket);
}
