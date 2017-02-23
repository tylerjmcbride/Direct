package github.tylerjmcbride.direct.registration.runnables;

import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.json.ServerJSON;
import github.tylerjmcbride.direct.listeners.ClientRegisteredListener;
import github.tylerjmcbride.direct.model.Device;

public class RegisterClientRunnable implements Runnable {

    private Socket clientSocket;
    private List<Device> registeredClients;
    private ClientRegisteredListener listener;

    public RegisterClientRunnable(Socket clientSocket, List<Device> registeredClients, ClientRegisteredListener listener) {
        this.clientSocket = clientSocket;
        this.registeredClients = registeredClients;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            DataInputStream from = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream to = new DataOutputStream(clientSocket.getOutputStream());
            final Device client = LoganSquare.parse(from.readUTF(), Device.class);

            if (!registeredClients.contains(client)) {
                registeredClients.add(client);
                to.writeUTF(LoganSquare.serialize(new ServerJSON()));
                to.flush();

                listener.onClientRegistered(client);
            } else {
                to.writeUTF("UNREGISTER_DIRECT_DEVICE");
                to.flush();

                registeredClients.remove(client);
                listener.onClientUnregistered(client);
            }

            from.close();
            to.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(Direct.TAG, "Failed to register client.");
        } finally {
            try {
                clientSocket.close();
            } catch (Exception ex) {
                Log.e(Direct.TAG, "Failed to close client socket.");
            }
        }
    }
}
