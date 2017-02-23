package github.tylerjmcbride.direct.listeners;

import java.net.ServerSocket;

/**
 * Interface for callback invocation on an action.
 */
public interface ServerSocketInitializationCompleteListener {
    void onSuccess(ServerSocket socket);
    void onFailure();
}
