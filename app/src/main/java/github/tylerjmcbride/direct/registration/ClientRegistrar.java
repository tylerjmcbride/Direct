package github.tylerjmcbride.direct.registration;

import android.os.Handler;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.Device;
import github.tylerjmcbride.direct.registration.runnables.ClientRegistrarRunnable;
import github.tylerjmcbride.direct.registration.runnables.ClientRegistrationSocketInitializationRunnable;

public class ClientRegistrar extends Registrar {

    ExecutorService executor = Executors.newSingleThreadExecutor();

    public ClientRegistrar(Direct direct, Handler handler) {
        super(direct, handler);
    }

    public void register(final Device thisDevice, InetSocketAddress address, final RegisteredWithServerListener listener) {
        new Thread(new ClientRegistrationSocketInitializationRunnable(address, BUFFER_SIZE, handler, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(Socket socket) {
                executor.submit(new ClientRegistrarRunnable(thisDevice, socket, handler, listener));
            }

            @Override
            public void onFailure() {
                listener.onFailure();
            }
        })).start();
    }
}
