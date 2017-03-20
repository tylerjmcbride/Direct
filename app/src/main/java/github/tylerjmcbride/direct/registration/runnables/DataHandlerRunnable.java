package github.tylerjmcbride.direct.registration.runnables;

import android.os.Handler;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.listeners.DataListener;
import github.tylerjmcbride.direct.model.data.Data;
import github.tylerjmcbride.direct.utilities.DataParser;

public class DataHandlerRunnable extends ServerSocketRunnable {

    private Handler handler;
    private DataListener listener;

    public DataHandlerRunnable(ServerSocket registrationSocket, Handler handler, DataListener listener) {
        super(registrationSocket, Executors.newFixedThreadPool(5));
        this.handler = handler;
        this.listener = listener;
    }

    @Override
    public void onConnected(final Socket clientSocket) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedInputStream clientStream = new BufferedInputStream(clientSocket.getInputStream());
                    final Data data = DataParser.parse(IOUtils.toString(clientStream, "UTF-8"));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(Direct.TAG, "Succeeded to receive data from client.");
                            listener.onReceived(data);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.e(Direct.TAG, "Failed to receive data from client.");
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