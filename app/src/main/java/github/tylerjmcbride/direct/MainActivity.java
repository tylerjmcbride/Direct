package github.tylerjmcbride.direct;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.Serializable;

import github.tylerjmcbride.direct.callbacks.ClientCallback;
import github.tylerjmcbride.direct.callbacks.ConnectionCallback;
import github.tylerjmcbride.direct.callbacks.DiscoveryCallback;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.callbacks.ServiceCallback;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;

public class MainActivity extends AppCompatActivity {

    private Button hostButton;
    private Button clientButton;
    private Button hostButtonend;
    private Button clientButtonend;
    private Button clientButtonconnect;
    private Button clientButtondisconnect;
    private Button clientButtonsend;
    private Button hostButtonsend;

    private Client client;
    private Host host;

    private static final String TAG = "TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        host = new Host(getApplication(), "UNIQUE_SERVICE_TAG", "UNIQUE_INSTANCE_TAG");

        client = new Client(getApplication(), "UNIQUE_SERVICE_TAG");

        clientButton = (Button) findViewById(R.id.client);

        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.startDiscovery(new DiscoveryCallback() {
                    @Override
                    public void onDiscovered(WifiP2pDevice hostDevice) {
                        Log.d(TAG, "New service discovered");
                    }
                }, new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to start discovery");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to start discovery");
                    }
                });
            }
        });

        clientButtonend = (Button) findViewById(R.id.clientend);

        clientButtonend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.stopDiscovery(new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to stop discovery");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to stop discovery");
                    }
                });
            }
        });

        client.getNearbyHosts();
        clientButtonconnect = (Button) findViewById(R.id.clientconnect);

        clientButtonconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pDevice hostDevice = client.getNearbyHosts().get(0);

                client.connect(hostDevice, new ObjectCallback() {
                    @Override
                    public void onReceived(Object object) {
                        Log.d(TAG, "Object received from host");
                    }
                }, new ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        Log.d(TAG, "Connected to host");
                    }

                    @Override
                    public void onDisconnected() {
                        Log.d(TAG, "Disconnected with host, perhaps the host discontinued the service?");
                    }
                }, new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to request connection");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to request connection");
                    }
                });
            }
        });

        clientButtondisconnect = (Button) findViewById(R.id.clientdisconnect);

        clientButtondisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.disconnect(new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to disconnect from the service");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to disconnect from the service");
                    }
                });
            }
        });

        hostButton = (Button) findViewById(R.id.host);

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.startService(new ObjectCallback() {
                    @Override
                    public void onReceived(Object object) {
                        Log.d(TAG, "Object received from client");
                    }
                }, new ClientCallback() {
                    @Override
                    public void onConnected(WifiP2pDevice clientDevice) {
                        Log.d(TAG, "Client has connected");
                    }

                    @Override
                    public void onDisconnected(WifiP2pDevice clientDevice) {
                        Log.d(TAG, "Client has disconnected");
                    }
                }, new ServiceCallback() {
                    @Override
                    public void onP2PGroupDisbanded() {
                        Log.d(TAG, "The service is no longer available");
                    }
                }, new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to create service");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to create service");
                    }
                });
            }
        });

        hostButtonend = (Button) findViewById(R.id.hostend);

        hostButtonend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.stopService(new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to stop service");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Succeeded to stop service");
                    }
                });
            }
        });

        clientButtonsend = (Button) findViewById(R.id.clientsend);

        clientButtonsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Serializable serializableObject = new String("lol");
                client.send(serializableObject, new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Succeeded to send object");
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Failed to send object");
                    }
                });
            }
        });

        hostButtonsend = (Button) findViewById(R.id.hostsend);

        hostButtonsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!host.getRegisteredClients().isEmpty()) {
                    Serializable serializableObject = new String("lol");
                    WifiP2pDevice clientDevice = host.getRegisteredClients().get(0);
                    host.send(clientDevice, serializableObject, new ResultCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Succeeded to send object");
                        }

                        @Override
                        public void onFailure() {
                            Log.d(TAG, "Failed to send object");
                        }
                    });
                }
            }
        });
    }

}
