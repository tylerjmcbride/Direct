package github.tylerjmcbride.direct;

import android.app.Application;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.tylerjmcbride.direct.receivers.HostBroadcastReceiver;

public class Host extends Direct implements WifiP2pManager.ConnectionInfoListener {

    private static final int SERVICE_BROADCASTING_INTERVAL = 10000;

    private Handler serviceBroadcastingHandler = new Handler();
    private WifiP2pDnsSdServiceInfo info;
    private Map<String, String> record = new HashMap();
    private boolean isHosting = false;

    private List<WifiP2pDevice> clients = new ArrayList();

    public Host(Application application, String service, int serverPort, String instance) {
        super(application, service, serverPort, instance);
        receiver = new HostBroadcastReceiver(manager, channel, this);

        record.put(SERVICE_NAME_TAG, service);
        record.put(LISTEN_PORT_TAG, Integer.toString(serverPort));
        record.put(INSTANCE_NAME_TAG, instance);
        info = WifiP2pDnsSdServiceInfo.newInstance(instance, service.concat("._tcp"), record);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        //TODO implement this functionality
    }

    public void startService(final WifiP2pManager.ActionListener listener) {
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to clear local service.");
                manager.addLocalService(channel, info, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to add local service.");
                        isHosting = true;
                        serviceBroadcastingHandler.postDelayed(serviceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);
                        listener.onSuccess();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "Failed to add local service.");
                        listener.onFailure(reason);
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to clear local services.");
                listener.onFailure(reason);
            }
        });
    }

    public void stopService(WifiP2pManager.ActionListener listener) {
        manager.removeLocalService(channel, info, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Succeeded to remove local service.");
                isHosting = false;
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to remove local service.");

            }
        });
    }

    private Runnable serviceBroadcastingRunnable = new Runnable() {
        @Override
        public void run() {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int error) {
                }
            });

            if(isHosting) {
                serviceBroadcastingHandler.postDelayed(serviceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);
            }
        }
    };
}
