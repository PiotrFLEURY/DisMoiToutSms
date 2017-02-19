package fr.piotr.dismoitoutsms.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import fr.piotr.dismoitoutsms.DisMoiToutSmsActivity;
import fr.piotr.dismoitoutsms.R;

/**
 * Created by piotr_000 on 05/03/2016.
 */
public class NotificationHelper {

    public static final int SERVICE_STARTED_ID = 1;
    public static final int STOPPED_BY_STEP_COUNTER = 2;

    public static void open(Context context, int id) {
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
                icon = R.drawable.ic_directions_run_white_24dp;
                break;
            default:
                break;
        }
        open(context, id, icon, title, text);
    }

    public static void open(Context context, int id, int icon, String title, String text) {

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,
                DisMoiToutSmsActivity.class), 0);

//        android.support.v4.app.NotificationCompat.Style inboxStyle = new NotificationCompat.InboxStyle().addLine(text).setBigContentTitle(title);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(icon)
//                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_activate_24dp))
//                        .setStyle(inboxStyle)
                ;
        mBuilder.setContentIntent(pendingIntent);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, DisMoiToutSmsActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(DisMoiToutSmsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        notificationManager.notify(id, mBuilder.build());
    }

    public static void close(Context context, int id) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }
}
