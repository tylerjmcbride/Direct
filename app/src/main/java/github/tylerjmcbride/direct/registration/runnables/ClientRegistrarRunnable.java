package github.tylerjmcbride.direct.registration.runnables;

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
    private RegisteredWithServerListener listener;

    public ClientRegistrarRunnable(Device thisDevice, Socket serverSocket, RegisteredWithServerListener listener) {
        this.thisDevice = thisDevice;
        this.serverSocket = serverSocket;
        this.listener = listener;
    }

    @Override
    public void run() {
        Device host = null;
        try {
            DataOutputStream to = new DataOutputStream(serverSocket.getOutputStream());
            DataInputStream from = new DataInputStream(serverSocket.getInputStream());

            to.writeUTF(LoganSquare.serialize(thisDevice));
            to.flush();

            host = LoganSquare.parse(from.readUTF(), Device.class);

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
        listener.onSuccess(host);
    }
}
