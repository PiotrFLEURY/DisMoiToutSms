package fr.piotr.dismoitoutsms.util;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;

/**
 * Created by piotr on 21/01/2018.
 *
 */

public class BluetoothHelper {

    public static final String TAG = "BluetoothHelper";

    private static final String HEADSET = TAG + ".HEADSET";
    private static final String A2DP = TAG + ".A2DP";

    public static final String INTENT_BLUETOOTH_HEADSET_FOUND = TAG + ".INTENT_BLUETOOTH_HEADSET_FOUND";
    public static final String EXTRA_BLUETOOTH_HEADSET = TAG + ".EXTRA_BLUETOOTH_HEADSET";

    public static void listBluetoothDevices(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            for (BluetoothDevice bondedDevice : bluetoothAdapter.getBondedDevices()) {
                checkHeadsetProfile(context, bluetoothAdapter, bondedDevice);
            }
        }
    }

    private static void logBluetoothDevice(Context context, String profileName, BluetoothDevice bondedDevice) {
        Log.d(TAG, String.format("Bluetooth device {profile:%s - addres:%s - name:%s}",
                profileName, bondedDevice.getAddress(), bondedDevice.getName()));
        Intent intent = new Intent(INTENT_BLUETOOTH_HEADSET_FOUND);
        intent.putExtra(EXTRA_BLUETOOTH_HEADSET, new BluetoothHeadsetDevice(profileName, bondedDevice.getAddress(), bondedDevice.getName()));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private static void checkHeadsetProfile(final Context context, final BluetoothAdapter mBluetoothAdapter, BluetoothDevice bluetoothDevice) {
        BluetoothProfile.ServiceListener mHeadsetProfileListener = new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    BluetoothHeadset mBluetoothHeadset = (BluetoothHeadset) proxy;
                    List<BluetoothDevice> devicesMatchingConnectionStates = mBluetoothHeadset.getDevicesMatchingConnectionStates(new int[]{
                            BluetoothProfile.STATE_CONNECTED,
                            BluetoothProfile.STATE_CONNECTING,
                            BluetoothProfile.STATE_DISCONNECTED,
                            BluetoothProfile.STATE_DISCONNECTING}
                    );
                    boolean found = false;
                    for (BluetoothDevice devicesMatchingConnectionState : devicesMatchingConnectionStates) {
                        if(devicesMatchingConnectionState.getAddress().equals(bluetoothDevice.getAddress())){
                            found = true;
                            logBluetoothDevice(context, HEADSET, devicesMatchingConnectionState);
                        }
                    }
                    if(!found){
                        checkAD2PProfile(context, mBluetoothAdapter, bluetoothDevice);
                    }
                }
                // Close proxy connection after use.
                mBluetoothAdapter.closeProfileProxy(profile, proxy);
            }

            @Override
            public void onServiceDisconnected(int profile) {
                //
            }
        };

        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(context, mHeadsetProfileListener, BluetoothProfile.HEADSET);
    }

    private static void checkAD2PProfile(final Context context, final BluetoothAdapter mBluetoothAdapter, BluetoothDevice bluetoothDevice) {
        BluetoothProfile.ServiceListener mA2DPProfileListener = new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if(profile == BluetoothProfile.A2DP) {
                    BluetoothA2dp mBluetoothA2dp = (BluetoothA2dp) proxy;
                    List<BluetoothDevice> devicesMatchingConnectionStates = mBluetoothA2dp.getDevicesMatchingConnectionStates(new int[]{
                            BluetoothProfile.STATE_CONNECTED,
                            BluetoothProfile.STATE_CONNECTING,
                            BluetoothProfile.STATE_DISCONNECTED,
                            BluetoothProfile.STATE_DISCONNECTING}
                    );
                    for (BluetoothDevice devicesMatchingConnectionState : devicesMatchingConnectionStates) {
                        if(devicesMatchingConnectionState.getAddress().equals(bluetoothDevice.getAddress())){
                            logBluetoothDevice(context, A2DP, devicesMatchingConnectionState);
                            return;
                        }
                    }
                }

                // Close proxy connection after use.
                mBluetoothAdapter.closeProfileProxy(profile, proxy);
            }

            @Override
            public void onServiceDisconnected(int profile) {
                //
            }
        };

        mBluetoothAdapter.getProfileProxy(context, mA2DPProfileListener, BluetoothProfile.A2DP);
    }
}
