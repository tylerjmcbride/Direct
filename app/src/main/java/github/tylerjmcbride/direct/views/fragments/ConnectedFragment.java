package github.tylerjmcbride.direct.views.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import github.tylerjmcbride.direct.Client;
import github.tylerjmcbride.direct.R;
import github.tylerjmcbride.direct.callbacks.ConnectionCallback;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.transceivers.callbacks.ObjectCallback;

public class ConnectedFragment extends Fragment {

    private Client client;
    private WifiP2pDevice hostDevice;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_connected, container, false);
        final WifiP2pDevice hostDevice = this.hostDevice;

        client.connect(hostDevice, new ObjectCallback() {
            @Override
            public void onReceived(Object object) {
                toast(String.format("Received: %s.", object.toString()));
            }
        }, new ConnectionCallback() {
            @Override
            public void onConnected() {
                toast(String.format("Connected with %s.", hostDevice.deviceAddress));
                setOnClickListeners(view);
            }

            @Override
            public void onDisconnected() {
                toast(String.format("Disconnected with %s.", hostDevice.deviceAddress));
                switchToClientFragment();
            }
        }, new ResultCallback() {
            @Override
            public void onSuccess() {
                toast(String.format("Succeeded to request connection with %s.", hostDevice.deviceAddress));
            }

            @Override
            public void onFailure() {
                toast(String.format("Failed to request connection with %s.", hostDevice.deviceAddress));
                switchToClientFragment();
            }
        });

        return view;
    }

    private void toast(String toast) {
        Toast.makeText(ConnectedFragment.this.getContext(), toast, Toast.LENGTH_SHORT).show();
    }

    private void setOnClickListeners(View view) {
        Button sendTextButton = (Button) view.findViewById(R.id.send_text);
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = ConnectedFragment.this.getActivity();
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
                        client.send(text, new ResultCallback() {
                            @Override
                            public void onSuccess() {
                                toast(String.format("Succeeded to send text to %s.", hostDevice.deviceAddress));
                            }

                            @Override
                            public void onFailure() {
                                toast(String.format("Failed to send text to %s.", hostDevice.deviceAddress));
                            }
                        });
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

        Button disconnectButton = (Button) view.findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.disconnect(new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        toast(String.format("Succeeded to request disconnect with %s.", hostDevice.deviceAddress));
                    }

                    @Override
                    public void onFailure() {
                        toast(String.format("Succeeded to request disconnect with %s.", hostDevice.deviceAddress));
                    }
                });
            }
        });
    }

    private void switchToClientFragment() {
        // Create fragment and give it an argument specifying the article it should show
        ClientFragment clientFragment = new ClientFragment();
        clientFragment.setClient(client);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, clientFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    public void setHostDevice(WifiP2pDevice hostDevice) {
        this.hostDevice = hostDevice;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
