package github.tylerjmcbride.direct.receivers;

import android.net.wifi.p2p.WifiP2pManager;

import github.tylerjmcbride.direct.Host;

public class HostBroadcastReceiver extends DirectBroadcastReceiver {

    public HostBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Host host) {
        super(manager, channel, host);
    }
}
