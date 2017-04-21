package github.tylerjmcbride.direct.views.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import github.tylerjmcbride.direct.Host;
import github.tylerjmcbride.direct.R;
import github.tylerjmcbride.direct.views.fragments.HostFragment;

public class HostActivity extends FragmentActivity {

    private Host host;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_host);
        host = new Host(getApplication(), DirectActivity.SERVICE_TAG, "MY_UNIQUE_INSTANCE_TAG");

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            HostFragment hostFragment = new HostFragment();
            hostFragment.setHost(host);

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            hostFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, hostFragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
    }
}
