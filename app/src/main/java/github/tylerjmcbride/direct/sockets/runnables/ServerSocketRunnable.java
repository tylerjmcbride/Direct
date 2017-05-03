package github.tylerjmcbride.direct.sockets.runnables;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;

/**
 * The {@link ServerSocketRunnable} will run on its respective {@link Thread} until
 * {@link ServerSocket#close()} is called, or alternatively {@link Thread#interrupt()} is called.
 */
public abstract class ServerSocketRunnable implements Runnable {

    private ExecutorService executor;
    protected ServerSocket serverSocket;

    public ServerSocketRunnable(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.executor = Executors.newFixedThreadPool(5);
    }

    @Override
    public void run() {
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
            Log.d(Direct.TAG, String.format("Succeeded to close socket on %d.", serverSocket.getLocalPort()));
            serverSocket.close();
        } catch (SocketException ex) {
            // This exception is used to interrupt the current thread
            Log.d(Direct.TAG, String.format("Succeeded to close socket on %d.", serverSocket.getLocalPort()));
        } catch (IOException ex) {
            Log.e(Direct.TAG, String.format("Unexpected exception thrown by socket %d.", serverSocket.getLocalPort()));
        }

        // Clean up executor service
        executor.shutdownNow();
    }

    /**
     * Will be invoked when a connection is established to the {@link ServerSocket}.
     * @param socket The {@link Socket} that has established connection.
     */
    public abstract void onConnected(Socket socket);
}
