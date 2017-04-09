package github.tylerjmcbride.direct.listeners;

/**
 * Interface for callback invocation on an action.
 */
public interface ResultCallback {
    void onSuccess();
    void onFailure();
}
