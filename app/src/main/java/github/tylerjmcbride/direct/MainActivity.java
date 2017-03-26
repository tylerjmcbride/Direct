package github.tylerjmcbride.direct;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import github.tylerjmcbride.direct.listeners.DataCallback;
import github.tylerjmcbride.direct.model.data.Data;
import github.tylerjmcbride.direct.model.data.HandshakeData;

public class MainActivity extends AppCompatActivity {

    private Button hostButton;
    private Button clientButton;
    private Button hostButtonend;
    private Button clientButtonend;
    private Button clientButtonconnect;
    private Button clientButtondisconnect;
    private Button clientButtonsend;

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

        clientButton = (Button) findViewById(R.id.client);

        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client = new Client(getApplication(), "JUKE", 60606, Build.MODEL + " " + Build.USER);
                client.startDiscovery(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        });

        clientButtonend = (Button) findViewById(R.id.clientend);

        clientButtonend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.stopDiscovery(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        });

        clientButtonconnect = (Button) findViewById(R.id.clientconnect);

        clientButtonconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(client.getNearbyHosts().size() > 0) {
                    client.connect(client.getNearbyHosts().get(0), new DataCallback() {
                        @Override
                        public void onReceived(Data data) {
                            Log.d(Direct.TAG, "CLIENT YOU GOT THE DATA HURRAY!");
                        }
                    }, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int reason) {

                        }
                    });
                }
            }
        });

        clientButtondisconnect = (Button) findViewById(R.id.clientdisconnect);

        clientButtondisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.disconnect(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        });

        hostButton = (Button) findViewById(R.id.host);

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host = new Host(getApplication(), "JUKE", 60606, Build.MODEL + " " + Build.USER);
                host.startService(new DataCallback() {
                    @Override
                    public void onReceived(Data data) {
                        Log.d(Direct.TAG, "HOST YOU GOT THE DATA HURRAY!");
                    }
                }, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        });

        hostButtonend = (Button) findViewById(R.id.hostend);

        hostButtonend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.stopService(new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        });

        clientButtonsend = (Button) findViewById(R.id.clientsend);

        clientButtonsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.sendData(new HandshakeData("lol", 0));
            }
        });
    }

}
