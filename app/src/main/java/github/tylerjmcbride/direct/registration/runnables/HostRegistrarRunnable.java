package github.tylerjmcbride.direct.registration.runnables;

import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.model.Device;

/**
 * The {@link HostRegistrarRunnable} registers incoming clients on its respective {@link Thread} until
 * {@link ServerSocket#close()} is called, or alternatively {@link Thread#interrupt()} is called.
 */
public class HostRegistrarRunnable extends ServerSocketRunnable {

    private Direct direct;
    private List<Device> registeredClients;

    public HostRegistrarRunnable(ServerSocket registrationSocket, Direct direct, List<Device> registeredClients) {
        super(registrationSocket, Executors.newFixedThreadPool(5));
        this.direct = direct;
        this.registeredClients = registeredClients;
    }

    @Override
    public void onConnected(final Socket clientSocket) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    DataInputStream from = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream to = new DataOutputStream(clientSocket.getOutputStream());

                    // Client sends details about their device
                    final Device client = LoganSquare.parse(from.readUTF(), Device.class);
                    registeredClients.add(client);
                    Log.d(Direct.TAG, registeredClients.toString());

                    // Reply by sending details about the host device
                    to.writeUTF(LoganSquare.serialize(direct.getThisDevice()));
                    to.flush();

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
        });
    }
}
