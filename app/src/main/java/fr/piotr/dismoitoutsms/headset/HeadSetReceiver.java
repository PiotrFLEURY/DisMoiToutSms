package fr.piotr.dismoitoutsms.headset;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

import static android.media.AudioManager.ACTION_HEADSET_PLUG;

/**
 * Created by piotr on 09/07/2017.
 *
 */

public class HeadSetReceiver extends AbstractHeadSetReceiver {

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


}
