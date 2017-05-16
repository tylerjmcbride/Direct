package github.tylerjmcbride.direct.sockets.listeners;

import java.net.ServerSocket;

/**
 * Interface for callback invocation on an action.
 */
public interface ServerSocketInitializationListener {
    void onSuccess(ServerSocket serverSocket);
    void onFailure();
}