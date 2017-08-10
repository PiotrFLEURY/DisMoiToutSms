package fr.piotr.dismoitoutsms.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import fr.piotr.dismoitoutsms.headset.BluetoothReceiver;
import fr.piotr.dismoitoutsms.headset.HeadSetReceiver;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

/**
 * Created by piotr on 09/07/2017.
 *
 */

public class DisMoiToutSmsService extends Service {

    public static final String TAG = "DisMoiToutSmsService";

    public static final String INTENT_ACTIVATE_FROM_NOTIFICATION = TAG + ".INTENT_ACTIVATE_FROM_NOTIFICATION";

    private HeadSetReceiver headSetReceiver;
    private BluetoothReceiver bluetoothReceiver;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case INTENT_ACTIVATE_FROM_NOTIFICATION:
                    activateFromNotification();
                    break;
            }
        }
    };

    private void activateFromNotification() {
        Intent service = new Intent(this, ServiceCommunicator.class);
        service.addFlags(Intent.FLAG_FROM_BACKGROUND);
        getApplicationContext().startService(service);
        NotificationHelper.close(this, NotificationHelper.HEADSET_PLUGGED_IN);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        headSetReceiver = new HeadSetReceiver();
        bluetoothReceiver = new BluetoothReceiver();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTIVATE_FROM_NOTIFICATION);
        getApplicationContext().registerReceiver(receiver, filter);
        getApplicationContext().registerReceiver(headSetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        IntentFilter bluetoothIntentFilter = new IntentFilter();
        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        //bluetoothIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getApplicationContext().registerReceiver(bluetoothReceiver, bluetoothIntentFilter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        getApplicationContext().unregisterReceiver(receiver);
        getApplicationContext().unregisterReceiver(headSetReceiver);
        getApplicationContext().unregisterReceiver(bluetoothReceiver);
        super.onDestroy();
    }
}
