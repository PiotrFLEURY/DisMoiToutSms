package fr.piotr.dismoitoutsms.headset;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

/**
 * Created by piotr on 08/08/2017.
 *
 */

public class BluetoothReceiver extends AbstractHeadSetReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()){
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                onHeadSetPluggedIn(context);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                onHeadSetPluggedOut(context);
                break;
            /*case BluetoothAdapter.ACTION_STATE_CHANGED:
                onStateChanged(context, intent);
                break;*/
        }
    }

    /*public void onStateChanged(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(device != null) {
            if (device.getBondState()== BluetoothDevice.BOND_BONDED) {
                onHeadSetPluggedIn(context);
            } else {
                onHeadSetPluggedOut(context);
            }
        }
    }*/

}
