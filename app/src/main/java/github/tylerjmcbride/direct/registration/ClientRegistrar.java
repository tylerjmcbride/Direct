package github.tylerjmcbride.direct.registration;

import android.os.Handler;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.RegisteredWithServerListener;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.model.data.HandshakeData;
import github.tylerjmcbride.direct.registration.runnables.SocketConnectionRunnable;

public class ClientRegistrar extends Registrar {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ClientRegistrar(Direct direct, Handler handler) {
        super(direct, handler);
    }

    public void register(InetSocketAddress address, final RegisteredWithServerListener registeredWithServerListener) {
        executor.submit(new SocketConnectionRunnable(address, BUFFER_SIZE, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(final Socket hostSocket) {
                try {
                    DataOutputStream to = new DataOutputStream(hostSocket.getOutputStream());
                    DataInputStream from = new DataInputStream(hostSocket.getInputStream());

                    // Send details about the client device
                    WifiP2pDeviceInfo info = direct.getThisDeviceInfo();
                    to.writeUTF(LoganSquare.serialize(new HandshakeData(info.getMacAddress(), info.getPort())));
                    to.flush();

                    // Retrieve details about the host device
                    String string = from.readUTF();
                    final HandshakeData data = LoganSquare.parse(string, HandshakeData.class);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            registeredWithServerListener.onSuccess(new WifiP2pDeviceInfo(data.getMacAddress(), hostSocket.getInetAddress(), data.getPort()));
                        }
                    });

                    from.close();
                    to.close();
                } catch (IOException ex) {
                    Log.e(Direct.TAG, "Failed to register with server.");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            registeredWithServerListener.onFailure();
                        }
                    });
                } finally {
                    try {
                        hostSocket.close();
                    } catch (Exception ex) {
                        Log.e(Direct.TAG, "Failed to close registration socket.");
                    }
                }
            }

            @Override
            public void onFailure() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        registeredWithServerListener.onFailure();
                    }
                });
            }
        }));
    }
}
