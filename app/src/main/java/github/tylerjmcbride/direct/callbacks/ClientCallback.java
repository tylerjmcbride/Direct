package github.tylerjmcbride.direct.callbacks;

import android.net.wifi.p2p.WifiP2pDevice;

public interface ClientCallback {
    void onConnected(WifiP2pDevice clientDevice);
    void onDisconnected(WifiP2pDevice clientDevice);
}
