package github.tylerjmcbride.direct.sockets;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import github.tylerjmcbride.direct.Direct;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.sockets.listeners.ServerSocketInitializationCompleteListener;

/**
 * Static class for server socket initialization.
 */
public final class ServerSockets {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Private constructor to enforce static class.
     */
    private ServerSockets() {

    }

    /**
     * Attempts to initialize the {@link ServerSocket}, a random port will be used if the given is port is unavailable.
     * @param port The port for the {@link ServerSocket} to listen on.
     * @param maxServerConnections The maximum number of connections allowed at any given time.
     * @param listener The {@link ResultCallback} to capture the result of the given method call.
     */
    public static void initializeServerSocket(final int port, final int maxServerConnections, final Handler handler, final ServerSocketInitializationCompleteListener listener) {
        // Attempt to initialize the server socket on the given port
        new Thread(new Runnable() {
            @Override
            public void run() {
                initialize(port, maxServerConnections, DEFAULT_BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
                    @Override
                    public void onSuccess(final ServerSocket serverSocket) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onSuccess(serverSocket);
                            }
                        });
                    }

                    @Override
                    public void onFailure() {

                        // Attempt to initialize server socket on a random port
                        initialize(0, maxServerConnections, DEFAULT_BUFFER_SIZE, new ServerSocketInitializationCompleteListener() {
                            @Override
                            public void onSuccess(final ServerSocket serverSocket) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSuccess(serverSocket);
                                    }
                                });
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
                        });
                    }
                });
            }
        }).start();
    }

    /**
     * Attempts to initialize the {@link ServerSocket}.
     * @param port The port for the {@link ServerSocket} to listen on.
     * @param maxServerConnections The maximum number of connections allowed at any given time.
     * @param bufferSize The size of the buffer.
     * @param listener The {@link ResultCallback} to capture the result of the given method call.
     */
    private static void initialize(int port, int maxServerConnections, int bufferSize, ServerSocketInitializationCompleteListener listener) {
        try {
            ServerSocket socket = new ServerSocket(port, maxServerConnections);
            socket.setReuseAddress(true);
            socket.setReceiveBufferSize(bufferSize);
            listener.onSuccess(socket);
        } catch (IOException ex) {
            Log.d(Direct.TAG, String.format("The port %d is unavailable.", port));
            listener.onFailure();
        }
    }
}
