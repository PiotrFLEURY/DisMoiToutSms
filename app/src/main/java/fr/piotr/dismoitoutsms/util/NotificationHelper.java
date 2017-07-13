package fr.piotr.dismoitoutsms.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import fr.piotr.dismoitoutsms.DisMoiToutSmsActivity;
import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;

/**
 * Created by piotr_000 on 05/03/2016.
 *
 */
public class NotificationHelper {

    public static final String TAG = "NotificationHelper";

    public static final String EXTRA_ACTION_ICON = TAG + ".EXTRA_ACTION_ICON";
    public static final String EXTRA_ACTION_TEXT = TAG + ".EXTRA_ACTION_TEXT";

    public static final int SERVICE_STARTED_ID = 1;
    public static final int STOPPED_BY_STEP_COUNTER = 2;
    public static final int HEADSET_PLUGGED_IN = 3;

    public static void open(Context context, int id, Intent ... intents) {
        String title = null;
        String text = null;
        int icon = R.drawable.iconeprincipaleblanche;

        switch (id) {
            case SERVICE_STARTED_ID:
                title = context.getResources().getString(R.string.service_notif_titre);
                text = context.getResources().getString(R.string.service_notif_texte);
                icon = R.drawable.iconeprincipaleblanche;
                break;
            case STOPPED_BY_STEP_COUNTER:
                title = context.getResources().getString(R.string.service_notif_titre);
                text = context.getResources().getString(R.string.stoppedByStepText);
                icon = R.drawable.ic_directions_run_24dp;
                break;
            case HEADSET_PLUGGED_IN:
                title = context.getResources().getString(R.string.service_notif_titre);
                text = context.getResources().getString(R.string.headset_notification_text);
                icon = R.drawable.ic_headset_white_24dp;
                break;
            default:
                break;
        }
        open(context, id, icon, title, text, intents);
    }

    private static void open(Context context, int id, int icon, String title, String text, Intent... intents) {

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,
                DisMoiToutSmsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(icon)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text))
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                ;
        for (Intent intent : intents) {
            int actionIcon = intent.getIntExtra(EXTRA_ACTION_ICON, -1);
            String actionText = intent.getStringExtra(EXTRA_ACTION_TEXT);
            mBuilder.addAction(actionIcon, actionText, PendingIntent.getBroadcast(context, 0, intent, 0));
        }
        mBuilder.setContentIntent(pendingIntent);



        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(DisMoiToutSmsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        Intent intent =  new Intent(context, DisMoiToutSmsActivity.class);
        stackBuilder.addNextIntent(intent);

        notificationManager.notify(id, mBuilder.build());
    }

    public static void close(Context context, int id) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }
}
