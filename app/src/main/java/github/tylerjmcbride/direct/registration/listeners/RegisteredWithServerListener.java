package github.tylerjmcbride.direct.registration.listeners;

import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;

public interface RegisteredWithServerListener {
    void onSuccess(WifiP2pDeviceInfo info);
    void onFailure();
}
