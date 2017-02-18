package github.tylerjmcbride.direct;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Looper;

public abstract class Direct {

    public static final String TAG = "Direct";
    public static final String LISTEN_PORT_TAG = "LISTEN_PORT";
    public static final String SERVICE_NAME_TAG = "SERVICE_NAME";
    public static final String INSTANCE_NAME_TAG = "INSTANCE_NAME";

    protected WifiP2pManager manager;
    protected Channel channel;
    protected BroadcastReceiver receiver;

    protected int serverPort;
    protected String service;
    protected String instance;

    protected WifiP2pDevice self = new WifiP2pDevice();

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

        final Context context = application.getApplicationContext();
        final Looper looper = context.getMainLooper();
        manager = (WifiP2pManager) context.getSystemService(context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, looper, new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                channel = manager.initialize(context, looper, this);
            }
        });
    }

    public void setSelf(WifiP2pDevice self) {
        this.self = self;
    }
}