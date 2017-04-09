package github.tylerjmcbride.direct.registration.listeners;

import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;

public interface HandshakeListener {
    void onHandshake(WifiP2pDeviceInfo clientInfo);
    void onAdieu(WifiP2pDeviceInfo clientInfo);
}
