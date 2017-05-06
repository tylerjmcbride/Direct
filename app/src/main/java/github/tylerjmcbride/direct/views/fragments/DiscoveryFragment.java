package github.tylerjmcbride.direct.views.fragments;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import github.tylerjmcbride.direct.Client;
import github.tylerjmcbride.direct.R;
import github.tylerjmcbride.direct.callbacks.DiscoveryCallback;
import github.tylerjmcbride.direct.callbacks.ResultCallback;
import github.tylerjmcbride.direct.views.adapters.WifiP2PDeviceAdapter;

public class DiscoveryFragment extends Fragment {

    private Client client;

    private ListView discoveryList;
    private WifiP2PDeviceAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_discovery, container, false);
        adapter = new WifiP2PDeviceAdapter(getContext(), new ArrayList<WifiP2pDevice>());

        discoveryList = (ListView) view.findViewById(R.id.discovery_list);
        discoveryList.setAdapter(adapter);
        discoveryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice hostDevice = (WifiP2pDevice) discoveryList.getAdapter().getItem(position);
                switchToConnectedFragment(hostDevice);
            }
        });


        client.startDiscovery(new DiscoveryCallback() {
            @Override
            public void onDiscovered(WifiP2pDevice hostDevice) {
                adapter.clear();
                adapter.addAll(client.getNearbyHosts());
                adapter.notifyDataSetChanged();
                toast(String.format("Found host device %s.", hostDevice.deviceAddress));
            }

            @Override
            public void onLost(WifiP2pDevice hostDevice) {
                adapter.clear();
                adapter.addAll(client.getNearbyHosts());
                adapter.notifyDataSetChanged();
                toast(String.format("Lost host device %s.", hostDevice.deviceAddress));
            }
        }, new ResultCallback() {
            @Override
            public void onSuccess() {
                toast("Succeeded to start service discovery.");
            }

            @Override
            public void onFailure() {
                switchToClientFragment();
            }
        });

        Button stopDiscoveryButton = (Button) view.findViewById(R.id.stop_discovery);
        stopDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.stopDiscovery(new ResultCallback() {
                    @Override
                    public void onSuccess() {
                        toast("Succeeded to stop service discovery.");
                        switchToClientFragment();
                    }

                    @Override
                    public void onFailure() {
                        toast("Failed to stop service discovery.");
                    }
                });
            }
        });

        return view;
    }

    private void toast(String toast) {
        Toast.makeText(DiscoveryFragment.this.getContext(), toast, Toast.LENGTH_SHORT).show();
    }

    private void switchToConnectedFragment(WifiP2pDevice hostDevice) {
        // Create fragment and give it an argument specifying the article it should show
        ConnectedFragment connectedFragment = new ConnectedFragment();
        connectedFragment.setClient(client);
        connectedFragment.setHostDevice(hostDevice);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, connectedFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
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

    public void setClient(Client client) {
        this.client = client;
    }
}
