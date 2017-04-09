package github.tylerjmcbride.direct.sockets.runnables;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.sockets.listeners.SocketInitializationCompleteListener;

/**
 * To prevent {@link android.os.NetworkOnMainThreadException}, the initialization of the client
 * registration socket will be done on a separate thread.
 */
public class SocketConnectionRunnable implements Runnable {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int SOCKET_TIMEOUT = 2000;
    private static final int MAX_SOCKET_ATTEMPTS = 15;

    private InetSocketAddress address;
    private SocketInitializationCompleteListener listener;

    /**
     * Attempts to initialize the {@link Socket}.
     * @param address The {@link InetSocketAddress} of the server socket.
     * @param listener The {@link ResultCallback} to capture the success of a given method call.
     */
    public SocketConnectionRunnable(InetSocketAddress address, SocketInitializationCompleteListener listener) {
        this.address = address;
        this.listener = listener;
    }

    @Override
    public void run() {
        connect(MAX_SOCKET_ATTEMPTS);
    }

    /**
     * Attempts to establish a connection to the respective {@link InetSocketAddress}. Will recurse
     * until <code>attemptsLeft</code> reaches 0, or a successful connection has been established.
     *
     * @param attemptsLeft The number of connection attempts remaining.
     */
    private void connect(int attemptsLeft) {
        try {
            Socket socket = new Socket();
            socket.connect(address, SOCKET_TIMEOUT);
            socket.setReceiveBufferSize(DEFAULT_BUFFER_SIZE);
            socket.setSendBufferSize(DEFAULT_BUFFER_SIZE);
            listener.onSuccess(socket);
        } catch(SocketTimeoutException e) {
            // Attempt to connect to socket once again
            if(attemptsLeft > 0) {
                Log.d(Direct.TAG, String.format("Failed to connect to %s, will attempt to retry.", address));
                connect(attemptsLeft - 1);
            } else {
                Log.d(Direct.TAG, String.format("Failed to connect to %s.", address));
                listener.onFailure();
            }
        } catch (IOException e) {
            listener.onFailure();
        }
    }
}
