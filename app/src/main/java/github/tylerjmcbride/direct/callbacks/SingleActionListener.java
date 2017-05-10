package github.tylerjmcbride.direct.callbacks;

import android.net.wifi.p2p.WifiP2pManager.ActionListener;

public abstract class SingleActionListener implements ActionListener {

    @Override
    public void onSuccess() {
        onSuccessOrFailure();
    }

    @Override
    public void onFailure(int reason) {
        onSuccessOrFailure();
    }

    public abstract void onSuccessOrFailure();
}
