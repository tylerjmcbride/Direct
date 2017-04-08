package github.tylerjmcbride.direct.utilities.runnables;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.ActionListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;

/**
 * To prevent {@link android.os.NetworkOnMainThreadException}, the initialization of the client
 * registration socket will be done on a separate thread.
 */
public class SocketConnectionRunnable implements Runnable {

    private static final int SOCKET_TIMEOUT = 2000;
    private static final int MAX_SOCKET_ATTEMPTS = 5;

    private InetSocketAddress address;
    private int bufferSize;
    private SocketInitializationCompleteListener listener;

    /**
     * Attempts to initialize the {@link Socket}.
     * @param address The {@link InetSocketAddress} of the server socket.
     * @param bufferSize The size of the buffer.
     * @param listener The {@link ActionListener} to capture the success of a given method call.
     */
    public SocketConnectionRunnable(InetSocketAddress address, int bufferSize, SocketInitializationCompleteListener listener) {
        this.address = address;
        this.bufferSize = bufferSize;
        this.listener = listener;
    }

    @Override
    public void run() {
        connect(MAX_SOCKET_ATTEMPTS);
    }

    private void connect(int attemptsLeft) {
        if(attemptsLeft > 0) {
            try {
                Socket socket = new Socket();
                socket.connect(address, SOCKET_TIMEOUT);
                socket.setReceiveBufferSize(bufferSize);
                socket.setSendBufferSize(bufferSize);
                listener.onSuccess(socket);
            } catch(SocketTimeoutException e) {
                Log.d(Direct.TAG, "Failed to connect to " + address + ", will attempt to retry.");

                // Attempt to connect to socket once again
                connect(--attemptsLeft);
            } catch (IOException e) {
                listener.onFailure();
            }
        } else {
            listener.onFailure();
        }
    }
}
