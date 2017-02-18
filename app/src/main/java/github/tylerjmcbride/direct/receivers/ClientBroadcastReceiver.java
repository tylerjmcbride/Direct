package github.tylerjmcbride.direct.receivers;

import android.net.wifi.p2p.WifiP2pManager;

import github.tylerjmcbride.direct.Client;

public class ClientBroadcastReceiver extends DirectBroadcastReceiver {

    public ClientBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Client client) {
        super(manager, channel, client);
    }
}