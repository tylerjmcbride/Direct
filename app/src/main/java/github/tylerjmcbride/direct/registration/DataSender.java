package github.tylerjmcbride.direct.registration;

import android.os.Handler;
import android.util.Log;

import com.bluelinelabs.logansquare.LoganSquare;

import org.apache.commons.io.Charsets;

import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.SocketInitializationCompleteListener;
import github.tylerjmcbride.direct.model.data.Data;
import github.tylerjmcbride.direct.registration.runnables.SocketConnectionRunnable;

public class DataSender {

    private static final int BUFFER_SIZE = 65536;

    private ExecutorService executor = Executors.newFixedThreadPool(5);
    private Handler handler;

    public DataSender(Handler handler) {
        this.handler = handler;
    }

    /**
     * Sends data, obviously.
     * @param listener The {@link SocketInitializationCompleteListener} to capture the result of
     *                 the initialization.
     */
    public void send(final Data data, InetSocketAddress address, final SocketInitializationCompleteListener listener) {
        executor.submit(new SocketConnectionRunnable(address, BUFFER_SIZE, new SocketInitializationCompleteListener() {
            @Override
            public void onSuccess(final Socket hostSocket) {
                try {
                    BufferedOutputStream hostStream = new BufferedOutputStream(hostSocket.getOutputStream());
                    hostStream.write(LoganSquare.serialize(data).getBytes(Charsets.UTF_8));
                    hostStream.flush();
                    hostStream.close();
                    Log.e(Direct.TAG, "Succeeded to send data to host socket.");
                } catch (Exception ex) {
                    Log.e(Direct.TAG, "Failed to send data to host socket.");
                } finally {
                    try {
                        hostSocket.close();
                    } catch (Exception ex) {
                        Log.e(Direct.TAG, "Failed to close data socket.");
                    }
                }
            }

            @Override
            public void onFailure() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFailure();
                    }
                });
            }
        }));
    }
}
