package github.tylerjmcbride.direct.listeners;

/**
 * Interface for callback invocation on an action.
 */
public interface ActionListener {
    void onSuccess();
    void onFailure();
}
