package github.tylerjmcbride.direct.sockets.runnables;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
            serverSocket.close();
        } catch (IOException e) { // Really a SocketException
            Log.d(Direct.TAG, String.format("Succeeded to close socket on %d.", serverSocket.getLocalPort()));
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
