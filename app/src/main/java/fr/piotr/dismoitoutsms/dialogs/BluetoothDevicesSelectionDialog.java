package fr.piotr.dismoitoutsms.dialogs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.util.BluetoothHeadsetDevice;
import fr.piotr.dismoitoutsms.util.BluetoothHelper;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

/**
 * Created by piotr on 21/01/2018.
 *
 */

public class BluetoothDevicesSelectionDialog extends AlertDialog implements DialogInterface.OnDismissListener {

    private class MyAdapter extends BaseAdapter {

        Context context;
        List<BluetoothHeadsetDevice> list = new ArrayList<>();

        public MyAdapter(Context context){
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
                adapter.add((BluetoothHeadsetDevice) intent.getSerializableExtra(BluetoothHelper.EXTRA_BLUETOOTH_HEADSET));
                adapter.notifyDataSetChanged();
            }
        }
    };

    ListView listView;
    MyAdapter adapter;

    public BluetoothDevicesSelectionDialog(@NonNull Context context) {
        super(context);

        View layout = LayoutInflater.from(context).inflate(R.layout.bluetooth_devices_dialog, getListView(), false);
        listView = layout.findViewById(R.id.lv_bluetooth_devices_dialog);
        adapter = new MyAdapter(context);
        listView.setAdapter(adapter);

        setView(layout);

        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver,
                        new IntentFilter(BluetoothHelper.INTENT_BLUETOOTH_HEADSET_FOUND));

        BluetoothHelper.listBluetoothDevices(context);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
    }
}
