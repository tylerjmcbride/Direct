package github.tylerjmcbride.direct.transceivers.runnables;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.sockets.ServerSocketRunnable;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationListener;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;

public class ObjectReceiverRunnable extends ServerSocketRunnable {

    private ObjectCallback objectCallback;

    public ObjectReceiverRunnable(int port, int maxServerConnections, Handler handler, ObjectCallback objectCallback, ServerSocketInitializationListener listener) {
        super(port, maxServerConnections, handler, listener);
        this.objectCallback = objectCallback;
    }

    @Override
    public void onConnected(final Socket connectedSocket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(connectedSocket.getInputStream()));
            final Object object = inputStream.readObject();
            Log.d(Direct.TAG, "Succeeded to receive data.");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    objectCallback.onReceived(object);
                }
            });
        } catch (ClassNotFoundException ex) {
            Log.e(Direct.TAG, "Failed to read data.");
        } catch (IOException ex) {
            Log.e(Direct.TAG, "Failed to receive data.");
        } finally {
            if (connectedSocket != null && connectedSocket.isConnected()) {
                try {
                    connectedSocket.close();
                } catch (IOException e) {
                    Log.e(Direct.TAG, "Failed to close socket.");
                }
            }
        }
    }
}