package github.tylerjmcbride.direct.callbacks;

/**
 * Interface for callback invocation on an action.
 */
public interface ResultCallback {
    void onSuccess();
    void onFailure();
}
