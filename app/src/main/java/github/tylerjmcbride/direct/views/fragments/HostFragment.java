package github.tylerjmcbride.direct.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import github.tylerjmcbride.direct.WifiDirectHost;
import github.tylerjmcbride.direct.R;
import github.tylerjmcbride.direct.views.activities.DirectActivity;

public class HostFragment extends Fragment {

    private WifiDirectHost host;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host, container, false);

        Button createServiceButton = (Button) view.findViewById(R.id.start_service);
        createServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // Create fragment and give it an argument specifying the article it should show
            ServiceFragment serviceFragment = new ServiceFragment();
            serviceFragment.setHost(host);

            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, serviceFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
            }
        });

        Button backButton = (Button) view.findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), DirectActivity.class));
            }
        });

        return view;
    }

    public void setHost(WifiDirectHost host) {
        this.host = host;
    }
}
