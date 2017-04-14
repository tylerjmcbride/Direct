package github.tylerjmcbride.direct.transceivers.runnables;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.sockets.runnables.ServerSocketRunnable;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;

public class ObjectReceiverRunnable extends ServerSocketRunnable {

    private Handler handler;
    private ObjectCallback objectCallback;

    public ObjectReceiverRunnable(ServerSocket registrationSocket, Handler handler, ObjectCallback dataCallback) {
        super(registrationSocket);
        this.handler = handler;
        this.objectCallback = dataCallback;
    }

    @Override
    public void onConnected(final Socket connectedSocket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(connectedSocket.getInputStream()));
            final Object object = inputStream.readObject();
            inputStream.close();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(Direct.TAG, "Succeeded to receive data.");
                    objectCallback.onReceived(object);
                }
            });
        } catch (Exception ex) {
            Log.e(Direct.TAG, "Failed to receive data.");
        } finally {
            if (connectedSocket != null) {
                if (connectedSocket.isConnected()) {
                    try {
                        connectedSocket.close();
                    } catch (IOException e) {
                        Log.e(Direct.TAG, "Failed to close socket.");
                    }
                }
            }
        }
    }
}