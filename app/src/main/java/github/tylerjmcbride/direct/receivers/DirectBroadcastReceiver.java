package github.tylerjmcbride.direct.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

/**
 * Wrapper class to eliminate the need for the ugly {@link BroadcastReceiver#onReceive(Context, Intent)}
 * method.
 */
public abstract class DirectBroadcastReceiver extends BroadcastReceiver {

    protected WifiP2pManager manager;
    protected Channel channel;

    public DirectBroadcastReceiver(WifiP2pManager manager, Channel channel) {
        super();
        this.manager = manager;
        this.channel = channel;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            stateChanged(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
            discoveryChanged(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            peersChanged();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pGroup p2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            connectionChanged(p2pInfo, networkInfo, p2pGroup);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            thisDeviceChanged(device);
        }
    }

    protected abstract void connectionChanged(WifiP2pInfo p2pInfo, NetworkInfo info, WifiP2pGroup p2pGroup);

    protected abstract void peersChanged();

    protected abstract void stateChanged(boolean wifiEnabled);

    protected abstract void discoveryChanged(boolean discoveryEnabled);

    protected abstract void thisDeviceChanged(WifiP2pDevice device);
}
