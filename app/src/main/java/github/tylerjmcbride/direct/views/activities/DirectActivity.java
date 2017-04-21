package github.tylerjmcbride.direct.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import github.tylerjmcbride.direct.R;

public class DirectActivity extends Activity {

    public static final String SERVICE_TAG = "MY_UNIQUE_SERVICE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_direct);

        Button hostButton = (Button) findViewById(R.id.host);
        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DirectActivity.this, HostActivity.class));
            }
        });

        Button discoverButton = (Button) findViewById(R.id.client);
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DirectActivity.this, ClientActivity.class));
            }
        });
    }
}
