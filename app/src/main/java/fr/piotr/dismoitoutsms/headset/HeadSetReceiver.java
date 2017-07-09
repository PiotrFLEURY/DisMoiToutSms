package fr.piotr.dismoitoutsms.headset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
        NotificationHelper.open(context, NotificationHelper.HEADSET_PLUGGED_IN);
    }
}
