package github.tylerjmcbride.direct.registration.runnables;

import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.ClientRegisteredListener;
import github.tylerjmcbride.direct.model.Device;

public class RegisterClientRunnable implements Runnable {

    private Direct direct;
    private Socket clientSocket;
    private List<Device> registeredClients;
    private ClientRegisteredListener listener;

    public RegisterClientRunnable(Socket clientSocket, Direct direct, List<Device> registeredClients, ClientRegisteredListener listener) {
        this.clientSocket = clientSocket;
        this.direct = direct;
        this.registeredClients = registeredClients;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            DataInputStream from = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream to = new DataOutputStream(clientSocket.getOutputStream());

            final Device client = LoganSquare.parse(from.readUTF(), Device.class);
            registeredClients.add(client);

            to.writeUTF(LoganSquare.serialize(direct.getThisDevice()));
            to.flush();

            listener.onClientRegistered(client);

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
