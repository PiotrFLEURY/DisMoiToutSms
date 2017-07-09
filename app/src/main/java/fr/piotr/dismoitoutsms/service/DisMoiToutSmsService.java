package fr.piotr.dismoitoutsms.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTIVATE_FROM_NOTIFICATION);
        getApplicationContext().registerReceiver(receiver, filter);
        getApplicationContext().registerReceiver(headSetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
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
        super.onDestroy();
    }
}
