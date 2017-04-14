package github.tylerjmcbride.direct.transceivers;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.sockets.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.sockets.runnables.SocketConnectionRunnable;

public class ObjectTransmitter {

    private ExecutorService executor = Executors.newFixedThreadPool(5);
    private Handler handler;

    public ObjectTransmitter(Handler handler) {
        this.handler = handler;
    }

    /**
     * Sends data to the respective address.
     * @param object The {@link Serializable} object to send.
     * @param address The {@link InetSocketAddress}.
     * @param listener The {@link SocketInitializationCompleteListener}.
     */
    public void send(final Serializable object, InetSocketAddress address, final SocketInitializationCompleteListener listener) {
        executor.submit(new SocketConnectionRunnable(address, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(final Socket socket) {
                try {
                    ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    outputStream.writeObject(object);
                    outputStream.flush();
                    outputStream.close();
                    Log.d(Direct.TAG, "Succeeded to send object.");
                } catch (IOException ex) {
                    Log.e(Direct.TAG, "Failed to send object.");
                } finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(Direct.TAG, "Failed to close socket.");
                            }
                        }
                    }
                }
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
}
