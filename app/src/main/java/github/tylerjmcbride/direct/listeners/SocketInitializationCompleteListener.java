package github.tylerjmcbride.direct.listeners;


import java.net.Socket;

/**
 * Interface for callback invocation on an action.
 */
public interface SocketInitializationCompleteListener {
    void onSuccess(Socket socket);
    void onFailure();
}
