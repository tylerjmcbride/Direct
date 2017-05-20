package github.tylerjmcbride.direct.sockets;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import github.tylerjmcbride.direct.WifiDirect;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.sockets.listeners.SocketInitializationCompleteListener;

/**
 * To prevent a {@link android.os.NetworkOnMainThreadException}, this runnable should not be run on
 * the main thread.
 */
public class SocketRunnable extends AbstractSocketRunnable implements Runnable {

    private static final int SOCKET_TIMEOUT = 2000;
    private static final int MAX_SOCKET_ATTEMPTS = 15;

    private InetSocketAddress address;
    private SocketInitializationCompleteListener listener;

    /**
     * Attempts to initialize the {@link Socket}.
     * @param address The {@link InetSocketAddress} of the server socket.
     * @param listener The {@link ResultCallback} to capture the success of a given method call.
     */
    public SocketRunnable(InetSocketAddress address, SocketInitializationCompleteListener listener) {
        this.address = address;
        this.listener = listener;
    }

    @Override
    public void run() {
        connect(MAX_SOCKET_ATTEMPTS - 1);
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
        } catch(IOException e) {
            // Attempt to connect to socket once again
            if(attemptsLeft > 0) {
                Log.d(WifiDirect.TAG, String.format("Failed to connect to %s, will attempt to retry.", address));
                connect(attemptsLeft - 1);
            } else {
                Log.d(WifiDirect.TAG, String.format("Failed to connect to %s.", address));
                listener.onFailure();
            }
        }
    }
}
