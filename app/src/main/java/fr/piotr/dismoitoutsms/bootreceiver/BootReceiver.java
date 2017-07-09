package fr.piotr.dismoitoutsms.bootreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;

/**
 * Created by piotr on 09/07/2017.
 *
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            //TODO start service if needed
            context.startService(new Intent(context, DisMoiToutSmsService.class));
        }
    }
}
