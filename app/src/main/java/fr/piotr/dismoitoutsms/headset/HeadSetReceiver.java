package fr.piotr.dismoitoutsms.headset;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

import static android.media.AudioManager.ACTION_HEADSET_PLUG;

/**
 * Created by piotr on 09/07/2017.
 *
 */

public class HeadSetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_HEADSET_PLUG)){
            int audioState = intent.getIntExtra("state", -1);
            if(audioState == 1) {
                onHeadSetPluggedIn(context);
            } else if(audioState==0){
                onHeadSetPluggedOut(context);
            }
        }
    }

    private void onHeadSetPluggedOut(Context context) {
        NotificationHelper.close(context, NotificationHelper.HEADSET_PLUGGED_IN);
    }

    private void onHeadSetPluggedIn(Context context) {
        if(!isMyServiceRunning(context)) {
            Intent intent = new Intent(DisMoiToutSmsService.INTENT_ACTIVATE_FROM_NOTIFICATION);
            intent.putExtra(NotificationHelper.EXTRA_ACTION_ICON, R.drawable.ic_headset_white_24dp);
            intent.putExtra(NotificationHelper.EXTRA_ACTION_TEXT, context.getString(R.string.activate));
            NotificationHelper.open(context, NotificationHelper.HEADSET_PLUGGED_IN, intent);
        }
    }

    public boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServiceCommunicator.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
