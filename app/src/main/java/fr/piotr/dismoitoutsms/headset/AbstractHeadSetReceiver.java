package fr.piotr.dismoitoutsms.headset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

import static fr.piotr.dismoitoutsms.util.NotificationHelper.HEADSET_PLUGGED_IN;

/**
 * Created by piotr on 08/08/2017.
 *
 */

public abstract class AbstractHeadSetReceiver extends BroadcastReceiver {

    private DisMoiToutSmsService disMoiToutSmsService;

    protected AbstractHeadSetReceiver(DisMoiToutSmsService disMoiToutSmsService) {
        this.disMoiToutSmsService = disMoiToutSmsService;
    }

    protected void onHeadSetPluggedOut(Context context) {
        if(disMoiToutSmsService.getServiceCommunicatorBound()) {
            if(onAutoStop()) {
                disMoiToutSmsService.stopServiceCommunicator();
            }
        } else {
            NotificationHelper.INSTANCE.close(context, HEADSET_PLUGGED_IN);
        }
    }

    protected void onHeadSetPluggedIn(Context context) {
        if(!disMoiToutSmsService.getServiceCommunicatorBound()) {
            if(isHeadsetModeActivated(context)){
                onAutoStart();
                disMoiToutSmsService.startServiceCommunicator();
            } else {
                notifyActivationPurpose(context);
            }
        }
    }

    protected void notifyActivationPurpose(Context context) {
        Intent intent = new Intent(DisMoiToutSmsService.Companion.getINTENT_ACTIVATE_FROM_NOTIFICATION());
        intent.putExtra(NotificationHelper.INSTANCE.getEXTRA_ACTION_ICON(), R.drawable.ic_headset_white_24dp);
        intent.putExtra(NotificationHelper.INSTANCE.getEXTRA_ACTION_TEXT(), context.getString(R.string.activate));
        NotificationHelper.INSTANCE.open(context, HEADSET_PLUGGED_IN, intent);
    }

    protected void onAutoStart(){
        //
    }

    protected boolean onAutoStop(){
        return true;
    }

    protected abstract boolean isHeadsetModeActivated(Context context);
}
