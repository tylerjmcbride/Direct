package github.tylerjmcbride.direct;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.registration.model.Handshake;
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

        client = new Client(getApplication(), "JUKE", Build.MODEL + " " + Build.USER);
        host = new Host(getApplication(), "JUKE", Build.MODEL + " " + Build.USER);

        clientButton = (Button) findViewById(R.id.client);

        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.startDiscovery(new ResultCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure() {

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

                    }

                    @Override
                    public void onFailure() {

                    }
                });
            }
        });

        clientButtonconnect = (Button) findViewById(R.id.clientconnect);

        clientButtonconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(client.getNearbyHosts().size() > 0) {
                    client.connect(client.getNearbyHosts().get(0), new ObjectCallback() {
                        @Override
                        public void onReceived(Object object) {
                            Log.d(Direct.TAG, "CLIENT YOU GOT THE DATA HURRAY!");
                        }
                    }, new ResultCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure() {

                        }
                    });
                }
            }
        });

        clientButtondisconnect = (Button) findViewById(R.id.clientdisconnect);

        clientButtondisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.disconnect(new ResultCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure() {

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
                        Log.d(Direct.TAG, "HOST YOU GOT THE DATA HURRAY!");
                    }
                }, new ResultCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure() {

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

                    }

                    @Override
                    public void onFailure() {

                    }
                });
            }
        });

        clientButtonsend = (Button) findViewById(R.id.clientsend);

        clientButtonsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.send(new Handshake("lol", 0), new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Succeeded Sending Data.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getApplicationContext(), "Failed Sending Data.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        hostButtonsend = (Button) findViewById(R.id.hostsend);

        hostButtonsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!host.getRegisteredClients().isEmpty()) {
                    WifiP2pDevice device = host.getRegisteredClients().get(0);
                    host.send(device, new Handshake("lol", 0), new ResultCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Succeeded Sending Data.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure() {
                            Toast.makeText(getApplicationContext(), "Failed Sending Data.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

}
