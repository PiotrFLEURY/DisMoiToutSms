package fr.piotr.dismoitoutsms.dialogs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.util.BluetoothHeadsetDevice;
import fr.piotr.dismoitoutsms.util.BluetoothHelper;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

import static android.content.Context.POWER_SERVICE;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.BLUETOOTH_HEADSET_MODE;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.setBoolean;

/**
 * Created by piotr on 21/01/2018.
 *
 */

public class BluetoothDevicesSelectionFragment extends Fragment {

    private class MyAdapter extends BaseAdapter {

        Context context;
        List<BluetoothHeadsetDevice> list = new ArrayList<>();

        MyAdapter(Context context){
            this.context = context;
        }

        public void add(BluetoothHeadsetDevice device){
            list.add(device);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public BluetoothHeadsetDevice getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.bluetooth_device, parent, false);
            }

            BluetoothHeadsetDevice bluetoothHeadsetDevice = getItem(position);

            TextView tvName = view.findViewById(R.id.tv_bluetooth_device_name);
            tvName.setText(bluetoothHeadsetDevice.getName());

            Switch swSelection = view.findViewById(R.id.switch_bluetooth_device_selection);
            swSelection.setOnCheckedChangeListener((buttonView, isChecked) -> ConfigurationManager.toggleBluetoothDevice(context, bluetoothHeadsetDevice.getAddress(), isChecked));
            swSelection.setChecked(!ConfigurationManager.isBluetoothBanned(context, bluetoothHeadsetDevice.getAddress()));

            return view;
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothHelper.INTENT_BLUETOOTH_HEADSET_FOUND.equals(intent.getAction())){
                BluetoothHeadsetDevice bluetoothHeadsetDevice = (BluetoothHeadsetDevice) intent.getSerializableExtra(BluetoothHelper.EXTRA_BLUETOOTH_HEADSET);
                onBluetoothDeviceFound(bluetoothHeadsetDevice);
            }
        }
    };

    public static final String TAG = "BluetoothDevicesSelectionFragment";

    TextView tvNoDeviceFound;
    Switch swBluetoothHeadsetMode;
    ListView listView;
    MyAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bluetooth_devices_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNoDeviceFound = view.findViewById(R.id.bluetooth_device_fragment_tv_no_device_found);
        swBluetoothHeadsetMode = view.findViewById(R.id.switch_bluetooth_headset_mode);
        listView = view.findViewById(R.id.bluetooth_device_fragment_lv_devices);
        adapter = new MyAdapter(getContext());
        listView.setAdapter(adapter);

        BluetoothHelper.listBluetoothDevices(getContext());

    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(receiver,
                        new IntentFilter(BluetoothHelper.INTENT_BLUETOOTH_HEADSET_FOUND));

        swBluetoothHeadsetMode.setChecked(getBoolean(getContext(), BLUETOOTH_HEADSET_MODE));
        swBluetoothHeadsetMode.setOnCheckedChangeListener((v, checked)->onCheckChanged(checked));
    }

    private void onCheckChanged(boolean checked) {
        setBoolean(getContext(), BLUETOOTH_HEADSET_MODE, checked);
        if(checked){
            setupBatteryOptimization();
        }
    }

    private void setupBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            PowerManager pm = (PowerManager) getContext().getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getActivity().getPackageName())) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(receiver);

        swBluetoothHeadsetMode.setOnCheckedChangeListener(null);
    }

    private void onBluetoothDeviceFound(BluetoothHeadsetDevice bluetoothHeadsetDevice) {
        adapter.add(bluetoothHeadsetDevice);
        adapter.notifyDataSetChanged();
        tvNoDeviceFound.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.VISIBLE);
    }

}
