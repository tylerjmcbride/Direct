package github.tylerjmcbride.direct.registration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.listeners.ActionListener;
import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.Device;
import github.tylerjmcbride.direct.registration.runnables.ClientRegistrarRunnable;

public class ClientRegistrar extends Registrar {

    ExecutorService executor = Executors.newSingleThreadExecutor();

    public void register(final Device thisDevice, InetSocketAddress address, final RegisteredWithServerListener listener) {
        initalizeRegistrationSocket(address, BUFFER_SIZE, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(Socket socket) {
                executor.submit(new ClientRegistrarRunnable(thisDevice, socket, listener));
            }

            @Override
            public void onFailure() {
                listener.onFailure();
            }
        });
    }

    /**
     * Attempts to initalize the registration {@link Socket}.
     * @param address The {@link InetSocketAddress} of the server socket.
     * @param bufferSize The size of the buffer.
     * @param listener The {@link ActionListener} to capture the success of a given method call.
     */
    private void initalizeRegistrationSocket(InetSocketAddress address, int bufferSize, SocketInitializationCompleteListener listener) {
        try {
            Socket socket = new Socket();
            socket.connect(address);
            socket.setReceiveBufferSize(bufferSize);
            socket.setSendBufferSize(bufferSize);
            listener.onSuccess(socket);
        } catch (IOException ex) {
            listener.onFailure();
        }
    }
}
