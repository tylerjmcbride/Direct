package github.tylerjmcbride.direct;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.model.WifiP2pDeviceInfo;
import github.tylerjmcbride.direct.transceivers.ObjectReceiver;
import github.tylerjmcbride.direct.transceivers.ObjectTransmitter;

public abstract class Direct {

    public static final String TAG = "Direct";
    public static final String SERVICE_NAME_TAG = "SERVICE_NAME";
    public static final String REGISTRAR_PORT_TAG = "PORT";

    protected WifiManager wifiManager;
    protected WifiP2pManager manager;
    protected Channel channel;
    protected BroadcastReceiver receiver;
    protected IntentFilter intentFilter;
    protected Handler handler;
    protected Context context;

    protected String service;

    protected ObjectTransmitter objectTransmitter;
    protected ObjectReceiver objectReceiver;

    protected WifiP2pDevice thisDevice;
    protected WifiP2pDeviceInfo thisDeviceInfo;

    /**
     * Constructor for the abstract class {@link Direct}.
     * @param application The {@link Application}.
     * @param service The service type.
     */
    public Direct(Application application, String service) {
        this.service = service;

        this.intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        this.context = application.getApplicationContext();
        final Looper looper = context.getMainLooper();

        this.handler = new Handler(looper);
        this.objectReceiver = new ObjectReceiver(handler);
        this.objectTransmitter = new ObjectTransmitter(handler);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(context, looper, new ChannelListener() {
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
     * This method will attempt to both remove the current {@link WifiP2pGroup} and forget it's
     * persistence.
     *
     * @param callback Invoked upon the success or failure of the request.
     */
    protected void removeGroup(final ResultCallback callback) {
        manager.requestGroupInfo(channel, new GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                if(group != null) {
                    manager.removeGroup(channel, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Succeeded to remove group.");

                            try {
                                deletePersistentGroup(group, new ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Succeeded to delete persistent group.");
                                        callback.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(int reason) {
                                        Log.d(TAG, "Failed to delete persistent group.");
                                        callback.onFailure();
                                    }
                                });
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                                Log.e(TAG, "Failed to delete persistent group");
                                callback.onFailure();
                            }
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "Failed to remove group.");
                            callback.onFailure();
                        }
                    });
                } else {
                    Log.d(TAG, "Succeeded to confirm no group exists.");
                    callback.onSuccess();
                }
            }
        });
    }

    /**
     * Through reflection, this method will attempt to forget the persistent group.
     *
     * @see <a href="http://stackoverflow.com/questions/23653707/forgetting-old-wifi-direct-connections"></a>
     * @param wifiP2pGroup The respective {@link WifiP2pGroup}.
     * @param listener Invoked upon the success or failure of the request.
     */
    protected void deletePersistentGroup(WifiP2pGroup wifiP2pGroup, final ActionListener listener) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getNetworkId = WifiP2pGroup.class.getMethod("getNetworkId");
        Integer networkId = (Integer) getNetworkId.invoke(wifiP2pGroup);
        Method deletePersistentGroup = WifiP2pManager.class.getMethod("deletePersistentGroup", WifiP2pManager.Channel.class, int.class, ActionListener.class);
        deletePersistentGroup.invoke(manager, channel, networkId, listener);
    }

    /**
     * If this instance is garbage collected, unregister the receiver.
     * @throws Throwable Throws any given exception.
     */
    @Override
    protected void finalize() throws Throwable {
        if(context != null) {
            context.unregisterReceiver(receiver);
        }
    }
}