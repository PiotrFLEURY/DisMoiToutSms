package fr.piotr.dismoitoutsms.headset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication;
import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

/**
 * Created by piotr on 08/08/2017.
 *
 */

public abstract class AbstractHeadSetReceiver extends BroadcastReceiver {

    protected void onHeadSetPluggedOut(Context context) {
        if(DisMoiToutSmsApplication.INSTANCE.serviceCommunicatorRunning()) {
            if(onAutoStop()) {
                Intent service = new Intent(context, ServiceCommunicator.class);
                service.addFlags(Intent.FLAG_FROM_BACKGROUND);
                context.stopService(service);
            }
        } else {
            NotificationHelper.close(context, NotificationHelper.HEADSET_PLUGGED_IN);
        }
    }

    protected void onHeadSetPluggedIn(Context context) {
        if(!DisMoiToutSmsApplication.INSTANCE.serviceCommunicatorRunning()) {
            if(ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.HEADSET_MODE)){
                onAutoStart();
                Intent service = new Intent(context, ServiceCommunicator.class);
                service.addFlags(Intent.FLAG_FROM_BACKGROUND);
                context.startService(service);
            } else {
                notifyActivationPurpose(context);
            }
        }
    }

    protected void notifyActivationPurpose(Context context) {
        Intent intent = new Intent(DisMoiToutSmsService.INTENT_ACTIVATE_FROM_NOTIFICATION);
        intent.putExtra(NotificationHelper.EXTRA_ACTION_ICON, R.drawable.ic_headset_white_24dp);
        intent.putExtra(NotificationHelper.EXTRA_ACTION_TEXT, context.getString(R.string.activate));
        NotificationHelper.open(context, NotificationHelper.HEADSET_PLUGGED_IN, intent);
    }

    protected void onAutoStart(){
        //
    }

    protected boolean onAutoStop(){
        return true;
    }

}
