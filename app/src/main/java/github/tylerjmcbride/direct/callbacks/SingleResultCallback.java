package github.tylerjmcbride.direct.callbacks;

public abstract class SingleResultCallback implements ResultCallback {
    @Override
    public void onSuccess() {
        onSuccessOrFailure();
    }

    @Override
    public void onFailure() {
        onSuccessOrFailure();
    }
    
    public abstract void onSuccessOrFailure();
}
