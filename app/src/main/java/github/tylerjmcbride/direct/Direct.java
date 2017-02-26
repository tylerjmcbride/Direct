package github.tylerjmcbride.direct;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Looper;

import github.tylerjmcbride.direct.model.Device;

public abstract class Direct {

    public static final String TAG = "Direct";
    public static final String SERVICE_NAME_TAG = "SERVICE_NAME";
    public static final String REGISTRATION_PORT_TAG = "REGISTRATION_PORT";

    protected WifiManager wifiManager;
    protected WifiP2pManager manager;
    protected Channel channel;
    protected BroadcastReceiver receiver;
    protected IntentFilter intentFilter;

    protected WifiP2pDeviceList nearbyPeers;
    protected int serverPort;
    protected String service;
    protected String instance;

    protected Device thisDevice = new Device();

    /**
     * Constructor for the abstract class {@link Direct}.
     * @param application
     * @param instance
     * @param service
     * @param serverPort
     */
    public Direct(Application application, String service, int serverPort, String instance) {
        this.serverPort = serverPort;
        this.service = service;
        this.instance = instance;

        this.intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        final Context context = application.getApplicationContext();
        final Looper looper = context.getMainLooper();

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, looper, new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                channel = manager.initialize(context, looper, this);
            }
        });
    }

    /**
     * Returns this {@link Device}.
     * @return This device.
     */
    public Device getThisDevice() {
        return new Device(thisDevice);
    }
}