package github.tylerjmcbride.direct;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.registration.DataReceiver;
import github.tylerjmcbride.direct.registration.DataSender;

public abstract class Direct {

    public static final String TAG = "Direct";
    public static final String SERVICE_NAME_TAG = "SERVICE_NAME";
    public static final String PORT_TAG = "PORT";

    protected WifiManager wifiManager;
    protected WifiP2pManager manager;
    protected Channel channel;
    protected BroadcastReceiver receiver;
    protected IntentFilter intentFilter;
    protected Handler handler;

    protected WifiP2pDeviceList nearbyPeers;
    protected int serverPort;
    protected String service;
    protected String instance;

    protected DataSender dataSender;
    protected DataReceiver dataReceiver;

    protected WifiP2pDevice thisDevice;
    protected WifiP2pDeviceInfo thisDeviceInfo;

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
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        final Context context = application.getApplicationContext();
        final Looper looper = context.getMainLooper();

        this.handler = new Handler(looper);
        this.dataReceiver = new DataReceiver(handler);
        this.dataSender = new DataSender(handler);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(context, looper, new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                Direct.this.channel = manager.initialize(context, looper, this);
            }
        });

        this.thisDeviceInfo = new WifiP2pDeviceInfo(wifiManager.getConnectionInfo().getMacAddress());
    }

    /**
     * Returns this {@link WifiP2pDevice}.
     * @return This device.
     */
    public WifiP2pDevice getThisDevice() {
        return new WifiP2pDevice(thisDevice);
    }

    /**
     * Returns this {@link WifiP2pDeviceInfo}.
     * @return The information about this device.
     */
    public WifiP2pDeviceInfo getThisDeviceInfo() {
        return new WifiP2pDeviceInfo(thisDeviceInfo);
    }

    /**
     * @see <a href="http://stackoverflow.com/questions/23653707/forgetting-old-wifi-direct-connections"></a>
     * @param wifiP2pGroup
     */
    protected void deletePersistentGroup(WifiP2pGroup wifiP2pGroup, final WifiP2pManager.ActionListener listener) {
        try {

            Method getNetworkId = WifiP2pGroup.class.getMethod("getNetworkId");
            Integer networkId = (Integer) getNetworkId.invoke(wifiP2pGroup);
            Method deletePersistentGroup = WifiP2pManager.class.getMethod("deletePersistentGroup",
                    WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);
            deletePersistentGroup.invoke(manager, channel, networkId, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Succeeded to delete persistent group.");
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed to delete persistent group");
                    listener.onFailure(reason);
                }
            });
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Could not delete persistent group");
            listener.onFailure(0);
        }
    }
}