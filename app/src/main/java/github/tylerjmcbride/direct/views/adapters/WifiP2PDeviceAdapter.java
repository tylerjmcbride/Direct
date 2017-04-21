package github.tylerjmcbride.direct.views.adapters;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import github.tylerjmcbride.direct.R;

public class WifiP2PDeviceAdapter extends ArrayAdapter<WifiP2pDevice> {
    // View lookup cache
    private static class ViewHolder {
        TextView instanceName;
        TextView deviceName;
    }

    public WifiP2PDeviceAdapter(Context context, List<WifiP2pDevice> devices) {
        super(context, R.layout.item_device, devices);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        // Check if an existing view is being reused, otherwise inflate the view
        WifiP2pDevice device = getItem(position);
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_device, parent, false);
            viewHolder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
            viewHolder.instanceName = (TextView) convertView.findViewById(R.id.device_address);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data from the data object via the viewHolder object
        // into the template view.
        viewHolder.deviceName.setText(device.deviceName);
        viewHolder.instanceName.setText(device.deviceAddress);
        // Return the completed view to render on screen
        return convertView;
    }
}
