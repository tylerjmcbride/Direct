package github.tylerjmcbride.direct.registration.runnables;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.ClientRegisteredListener;
import github.tylerjmcbride.direct.model.Device;

/**
 * The {@link HostRegistrarRunnable} registers incoming clients on its respective {@link Thread} until
 * {@link ServerSocket#close()} is called, or alternatively {@link Thread#interrupt()} is called.
 */
public class HostRegistrarRunnable implements Runnable {

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    private Direct direct;
    private ServerSocket registrationSocket;
    private List<Device> registeredClients;
    private ClientRegisteredListener listener;

    public HostRegistrarRunnable(ServerSocket registrationSocket, Direct direct, List<Device> registeredClients, ClientRegisteredListener listener) {
        this.registrationSocket = registrationSocket;
        this.direct = direct;
        this.registeredClients = registeredClients;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = registrationSocket.accept();
                executor.submit(new RegisterClientRunnable(clientSocket, direct, registeredClients, listener));
            }
            // Current thread has been interrupted, clean up registration socket
            registrationSocket.close();
        } catch (IOException e) { // Really a SocketException
            Log.d(Direct.TAG, String.format("Succeeded to close registration socket on port %d.", registrationSocket.getLocalPort()));
        }

        // Clean up executor service
        executor.shutdownNow();
    }
}
