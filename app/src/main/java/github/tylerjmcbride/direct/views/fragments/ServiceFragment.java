package github.tylerjmcbride.direct.views.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import github.tylerjmcbride.direct.WifiDirectHost;
import github.tylerjmcbride.direct.R;
import github.tylerjmcbride.direct.callbacks.ClientCallback;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.callbacks.ServiceCallback;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;

public class ServiceFragment extends Fragment {

    private WifiDirectHost host;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_service, container, false);

        host.startService(new ObjectCallback() {
            @Override
            public void onReceived(Object object) {
                toast(String.format("Received: %s", object.toString()));
            }
        }, new ClientCallback() {
            @Override
            public void onConnected(WifiP2pDevice clientDevice) {
                toast(String.format("Client device %s has connected.", clientDevice.deviceAddress));
            }

            @Override
            public void onDisconnected(WifiP2pDevice clientDevice) {
                toast(String.format("Client device %s has disconnected.", clientDevice.deviceAddress));
            }
        }, new ServiceCallback() {
            @Override
            public void onServiceStopped() {
                toast("The service has stopped.");
                switchActivity();
            }
        }, new ResultCallback() {
            @Override
            public void onSuccess() {
                toast("The service has started.");
                setOnClickListeners(view);
            }

            @Override
            public void onFailure() {
                toast("The service has failed to start. Try restarting your Wi-Fi.");
                switchActivity();
            }
        });

        return view;
    }

    private void setOnClickListeners(View view) {
        Button sendTextButton = (Button) view.findViewById(R.id.send_text);
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Context context = ServiceFragment.this.getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Enter Your Message");

            // Set up the input
            final EditText input = new EditText(context);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String text = input.getText().toString();
                    for(final WifiP2pDevice clientDevice : host.getRegisteredClients()) {
                        host.send(clientDevice, text, new ResultCallback() {
                            @Override
                            public void onSuccess() {
                                toast(String.format("Succeeded to send client device %s the text.", clientDevice.deviceAddress));
                            }

                            @Override
                            public void onFailure() {
                                toast(String.format("Failed to send client device %s the text.", clientDevice.deviceAddress));
                            }
                        });
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
            }
        });

        Button stopServiceButton = (Button) view.findViewById(R.id.stop_service);
        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                host.stopService(new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        toast("Requested to stop the service.");
                    }

                    @Override
                    public void onFailure() {
                        toast("Failed to stop the service.");
                    }
                });
            }
        });
    }

    private void switchActivity() {
        // Create fragment and give it an argument specifying the article it should show
        HostFragment hostFragment = new HostFragment();
        hostFragment.setHost(host);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, hostFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    private void toast(String toast) {
        Toast.makeText(ServiceFragment.this.getContext(), toast, Toast.LENGTH_SHORT).show();
    }

    public void setHost(WifiDirectHost host) {
        this.host = host;
    }
}
