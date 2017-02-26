package github.tylerjmcbride.direct.registration;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.ActionListener;
import github.tylerjmcbride.direct.listeners.ClientRegisteredListener;
import github.tylerjmcbride.direct.listeners.ServerSocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.Device;
import github.tylerjmcbride.direct.registration.runnables.HostRegistrarRunnable;

/**
 * A {@link HostRegistrar} is in charge of handling the registration of client {@link WifiP2pDevice}s.
 */
public class HostRegistrar extends Registrar {

    private static final int DEFAULT_REGISTRATION_PORT = 37500;
    private static final int MAX_SERVER_CONNECTIONS = 25;

    private Thread registrationThread;
    private ServerSocket serverSocket;

    private List<Device> registeredClients = Collections.synchronizedList(new ArrayList<Device>());

    public HostRegistrar(Direct direct) {
        super(direct);
    }

    /**
     * Starts the registration process.
     * @param listener The {@link ServerSocketInitializationCompleteListener} to capture the success of
     *                 the initialization.
     * @param clientRegisteredListener The {@link ClientRegisteredListener} to capture the
     *                                 event of a client registration.
     */
    public void start(final ServerSocketInitializationCompleteListener listener, final ClientRegisteredListener clientRegisteredListener) {
        // Attempt to initalize registration on a default port
        initalizeRegistrationSocket(DEFAULT_REGISTRATION_PORT, MAX_SERVER_CONNECTIONS, BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
            @Override
            public void onSuccess(ServerSocket socket) {
                Log.d(Direct.TAG, String.format("Succeeded to initialize registration socket on port %d.", DEFAULT_REGISTRATION_PORT));
                serverSocket = socket;
                registrationThread = new Thread(new HostRegistrarRunnable(socket, direct, registeredClients, clientRegisteredListener));
                registrationThread.start();
                listener.onSuccess(socket);
            }

            @Override
            public void onFailure() {
                Log.d(Direct.TAG, String.format("Failed to initialize registration socket on port %d.", DEFAULT_REGISTRATION_PORT));

                // Attempt to initalize registration on a random port
                initalizeRegistrationSocket(0, MAX_SERVER_CONNECTIONS, BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
                    @Override
                    public void onSuccess(ServerSocket socket) {
                        Log.d(Direct.TAG, String.format("Succeeded to initialize registration socket on port %d.", socket.getLocalPort()));
                        serverSocket = socket;
                        registrationThread = new Thread(new HostRegistrarRunnable(socket, direct, registeredClients, clientRegisteredListener));
                        registrationThread.start();
                        listener.onSuccess(socket);
                    }

                    @Override
                    public void onFailure() {
                        Log.d(Direct.TAG, "Failed to initialize registration socket on random port.");
                        listener.onFailure();
                    }
                });
            }
        });
    }

    /**
     * Stops the registration process.
     */
    public void stop() {
        if(serverSocket != null) {
            try {
                serverSocket.close();
                registeredClients.clear();
                Log.d(Direct.TAG, "Succeeded to stop registrar.");
            } catch (IOException e) {
                // TODO Research when this would happen
            }
        }
    }

    /**
     * Attempts to initalize the registration {@link ServerSocket}.
     * @param registrationPort The port for the {@link ServerSocket} to listen on.
     * @param maxServerConnections The maximum number of connections allowed at any given time.
     * @param bufferSize The size of the buffer.
     * @param listener The {@link ActionListener} to capture the success of a given method call.
     */
    private void initalizeRegistrationSocket(int registrationPort, int maxServerConnections, int bufferSize, ServerSocketInitializationCompleteListener listener) {
        try {
            ServerSocket socket = new ServerSocket(registrationPort, maxServerConnections);
            socket.setReuseAddress(true);
            socket.setReceiveBufferSize(bufferSize);
            listener.onSuccess(socket);
        } catch (IOException ex) {
            Log.d(Direct.TAG, String.format("The port %d is unavailable.", registrationPort));
            listener.onFailure();
        }
    }
}
