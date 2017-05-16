package github.tylerjmcbride.direct.registration.runnables;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.Host;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.registration.listeners.HandshakeListener;
import github.tylerjmcbride.direct.registration.model.Adieu;
import github.tylerjmcbride.direct.registration.model.Handshake;
import github.tylerjmcbride.direct.sockets.ServerSocketRunnable;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationListener;

public class HostRegistrarRunnable extends ServerSocketRunnable {

    private Host host;
    private HandshakeListener handshakeListener;

    public HostRegistrarRunnable(int port, int maxServerConnections, Handler handler, Host host, HandshakeListener handshakeListener, ServerSocketInitializationListener listener) {
        super(port, maxServerConnections, handler, listener);
        this.host = host;
        this.handshakeListener = handshakeListener;
    }

    @Override
    public void onConnected(Socket clientSocket) {
        try {
            // Retrieve details about the client device
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Object object = inputStream.readObject();

            if(object instanceof Handshake) {
                Handshake handshake = (Handshake) object;
                final WifiP2pDeviceInfo clientInfo = new WifiP2pDeviceInfo(handshake.getMacAddress(), clientSocket.getInetAddress(), handshake.getPort());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handshakeListener.onHandshake(clientInfo);
                    }
                });

                // Send details about the host device
                WifiP2pDeviceInfo info = host.getThisDeviceInfo();
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                outputStream.writeObject(new Handshake(info.getMacAddress(), info.getPort()));
                outputStream.flush();
                outputStream.close();
            } else if(object instanceof Adieu) {
                Adieu adieu = (Adieu) object;
                final WifiP2pDeviceInfo clientInfo = new WifiP2pDeviceInfo(adieu.getMacAddress(), clientSocket.getInetAddress(), adieu.getPort());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handshakeListener.onAdieu(clientInfo);
                    }
                });
            }
        } catch (ClassNotFoundException ex) {
            Log.e(Direct.TAG, "Failed to read client registration data.");
        } catch (IOException ex) {
            Log.e(Direct.TAG, "Failed to complete registration transaction.");
        } finally {
            if (clientSocket != null && clientSocket.isConnected()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Log.e(Direct.TAG, "Failed to close client socket.");
                }
            }
        }
    }
}
