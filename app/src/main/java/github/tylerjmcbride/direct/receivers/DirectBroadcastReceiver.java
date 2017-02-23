package github.tylerjmcbride.direct.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
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
            stateChanged(context, intent, state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            peersChanged(context, intent);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            connectionChanged(context, intent, info);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            thisDeviceChanged(context, intent, device);
        }
    }

    protected abstract void connectionChanged(Context context, Intent intent, NetworkInfo info);

    protected abstract void peersChanged(Context context, Intent intent);

    protected abstract void stateChanged(Context context, Intent intent, boolean wifiEnabled);

    protected abstract void thisDeviceChanged(Context context, Intent intent, WifiP2pDevice device);
}
