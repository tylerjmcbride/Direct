package github.tylerjmcbride.direct.listeners;

import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;

public interface HandshakeListener {
    void onHandshake(WifiP2pDeviceInfo info);
}
