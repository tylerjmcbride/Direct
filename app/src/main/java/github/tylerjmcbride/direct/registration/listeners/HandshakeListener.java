package github.tylerjmcbride.direct.registration.listeners;

import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;

public interface HandshakeListener {
    void onClientAttemptingToRegister(WifiP2pDeviceInfo clientInfo);
    void onClientAttemptingToUnregister(WifiP2pDeviceInfo clientInfo);
}
