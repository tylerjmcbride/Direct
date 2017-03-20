package github.tylerjmcbride.direct.registration.runnables;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import github.tylerjmcbride.direct.Direct;

/**
 * The {@link ServerSocketRunnable} will run on its respective {@link Thread} until
 * {@link ServerSocket#close()} is called, or alternatively {@link Thread#interrupt()} is called.
 */
public abstract class ServerSocketRunnable implements Runnable {

    protected ExecutorService executor;
    protected ServerSocket serverSocket;

    public ServerSocketRunnable(ServerSocket serverSocket, ExecutorService executor) {
        this.serverSocket = serverSocket;
        this.executor = executor;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                onConnected(serverSocket.accept());
            }
            // Current thread has been interrupted, clean up registration socket
            serverSocket.close();
        } catch (IOException e) { // Really a SocketException
            Log.d(Direct.TAG, String.format("Succeeded to close registration socket on dataPort %d.", serverSocket.getLocalPort()));
        }

        // Clean up executor service
        executor.shutdownNow();
    }

    public abstract void onConnected(Socket clientSocket);
}
