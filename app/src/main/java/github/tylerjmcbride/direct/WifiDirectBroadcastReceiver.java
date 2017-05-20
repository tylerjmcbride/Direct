package github.tylerjmcbride.direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Wrapper class to eliminate the need for the ugly {@link BroadcastReceiver#onReceive(Context, Intent)}
 * method. This class is package private as it is only intended to be used internally.
 */
abstract class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            handleWifiP2pStateChangedAction(intent);
        } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            handleWifiP2pDiscoveryChangedAction(intent);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            handleWifiP2pPeersChangedAction();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            handleWifiP2pConnectionChangedAction(intent);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            handleWifiP2pThisDeviceChangedAction(intent);
        }
    }

    private void handleWifiP2pStateChangedAction(Intent intent) {
        int wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        if(wifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            onWifiP2pEnabled();
        } else if (wifiState == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
            onWifiP2pDisabled();
        }
    }

    private void handleWifiP2pDiscoveryChangedAction(Intent intent) {
        int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
        if(discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
            onPeerDiscoveryStarted();
        } else {
            onPeerDiscoveryStopped();
        }
    }

    private void handleWifiP2pPeersChangedAction() {
        onAvailablePeersChanged();
    }

    private void handleWifiP2pConnectionChangedAction(Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        onConnectionChanged(networkInfo);
    }

    private void handleWifiP2pThisDeviceChangedAction(Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        onThisDeviceChanged(device);
    }

    protected abstract void onConnectionChanged(NetworkInfo networkInfo);

    protected abstract void onAvailablePeersChanged();

    protected abstract void onWifiP2pEnabled();

    protected abstract void onWifiP2pDisabled();

    protected abstract void onPeerDiscoveryStarted();

    protected abstract void onPeerDiscoveryStopped();

    protected abstract void onThisDeviceChanged(WifiP2pDevice thisDevice);
}
