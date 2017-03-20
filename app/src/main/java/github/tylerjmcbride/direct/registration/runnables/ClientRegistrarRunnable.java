package github.tylerjmcbride.direct.registration.runnables;

import android.os.Handler;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.model.Device;

public class ClientRegistrarRunnable implements Runnable {

    private Device thisDevice;
    private Socket serverSocket;
    private Handler handler;
    private RegisteredWithServerListener listener;

    public ClientRegistrarRunnable(Device thisDevice, Socket serverSocket, Handler handler, RegisteredWithServerListener listener) {
        this.thisDevice = thisDevice;
        this.serverSocket = serverSocket;
        this.handler = handler;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            DataOutputStream to = new DataOutputStream(serverSocket.getOutputStream());
            DataInputStream from = new DataInputStream(serverSocket.getInputStream());

            // Send details about the client device
            to.writeUTF(LoganSquare.serialize(thisDevice));
            to.flush();

            // Retrieve details about the host device
            final Device host = LoganSquare.parse(from.readUTF(), Device.class);
            Log.d(Direct.TAG, host.toString());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess(host);
                }
            });

            from.close();
            to.close();
        } catch (IOException ex) {
            Log.e(Direct.TAG, "Failed to register with server.");
            listener.onFailure();
        } finally {
            try {
                serverSocket.close();
            } catch (Exception ex) {
                Log.e(Direct.TAG, "Failed to close registration socket.");
            }
        }
    }
}
