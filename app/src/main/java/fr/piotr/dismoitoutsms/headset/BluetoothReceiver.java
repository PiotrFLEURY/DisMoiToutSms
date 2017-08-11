package fr.piotr.dismoitoutsms.headset;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Created by piotr on 08/08/2017.
 *
 */

public class BluetoothReceiver extends AbstractHeadSetReceiver {

    final Handler handler = new Handler();

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(device != null) {
            switch(intent.getAction()){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    postDelayCheck(context);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    checkBluetoothHeadSetState(context);
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    onStateChanged(state);
                    break;
            }
        }


    }

    private void postDelayCheck(final Context context) {
        handler.postDelayed(() -> checkBluetoothHeadSetState(context), 3000);
    }

    private void onStateChanged(int state) {
        switch(state) {
            case BluetoothAdapter.STATE_OFF:
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                break;
            case BluetoothAdapter.STATE_ON:
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                break;
        }
    }

    private void checkBluetoothHeadSetState(Context context) {

        // Get the default adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        checkHeadsetProfile(context, mBluetoothAdapter);

    }

    private void checkHeadsetProfile(final Context context, final BluetoothAdapter mBluetoothAdapter) {
        BluetoothProfile.ServiceListener mHeadsetProfileListener = new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    BluetoothHeadset mBluetoothHeadset = (BluetoothHeadset) proxy;
                    boolean connected = !mBluetoothHeadset.getConnectedDevices().isEmpty();
                    onAudioEvent(context, connected);
                    if(!connected){
                        checkAD2PProfile(context, mBluetoothAdapter);
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

    private void checkAD2PProfile(final Context context, final BluetoothAdapter mBluetoothAdapter) {
        BluetoothProfile.ServiceListener mA2DPProfileListener = new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if(profile == BluetoothProfile.A2DP) {
                    BluetoothA2dp mBluetoothA2dp = (BluetoothA2dp) proxy;
                    onAudioEvent(context, !mBluetoothA2dp.getConnectedDevices().isEmpty());
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

    private void onAudioEvent(Context context, boolean connected){
        if(connected){
            onHeadSetPluggedIn(context);
        } else {
            onHeadSetPluggedOut(context);
        }
    }

}
