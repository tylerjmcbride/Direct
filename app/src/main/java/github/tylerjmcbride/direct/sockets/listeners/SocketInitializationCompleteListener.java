package github.tylerjmcbride.direct.sockets.listeners;

import java.net.Socket;

/**
 * Interface for callback invocation on an action.
 */
public interface SocketInitializationCompleteListener {
    void onSuccess(Socket socket);
    void onFailure();
}