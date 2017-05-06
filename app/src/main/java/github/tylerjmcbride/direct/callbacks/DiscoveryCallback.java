package github.tylerjmcbride.direct.callbacks;

import android.net.wifi.p2p.WifiP2pDevice;

public interface DiscoveryCallback {
    void onDiscovered(WifiP2pDevice hostDevice);
    void onLost(WifiP2pDevice hostDevice);
}
