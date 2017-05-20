package github.tylerjmcbride.direct.callbacks;

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;

public interface ConnectionAndGroupInfoAvailableListener {
    void onConnectionAndGroupInfoAvailable(WifiP2pInfo p2pInfo, WifiP2pGroup p2pGroup);
}
