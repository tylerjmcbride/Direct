package github.tylerjmcbride.direct.registration.runnables;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.ActionListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;

/**
 * To prevent {@link android.os.NetworkOnMainThreadException}, the initialization of the client
 * registration socket will be done on a separate thread.
 */
public class ClientRegistrationSocketInitializationRunnable implements Runnable {

    private InetSocketAddress address;
    private int bufferSize;
    private Handler handler;
    private SocketInitializationCompleteListener listener;

    /**
     * Attempts to initalize the registration {@link Socket}.
     * @param address The {@link InetSocketAddress} of the server socket.
     * @param bufferSize The size of the buffer.
     * @param handler The main looper handler.
     * @param listener The {@link ActionListener} to capture the success of a given method call.
     */
    public ClientRegistrationSocketInitializationRunnable(InetSocketAddress address, int bufferSize, Handler handler, SocketInitializationCompleteListener listener) {
        this.address = address;
        this.bufferSize = bufferSize;
        this.handler = handler;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            final Socket socket = new Socket();
            socket.connect(address);
            socket.setReceiveBufferSize(bufferSize);
            socket.setSendBufferSize(bufferSize);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess(socket);
                }
            });
        } catch (IOException ex) {
            Log.e(Direct.TAG, ex.getMessage());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFailure();
                }
            });
        }
    }
}
