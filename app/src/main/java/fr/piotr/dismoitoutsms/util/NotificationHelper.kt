package fr.piotr.dismoitoutsms.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import fr.piotr.dismoitoutsms.DisMoiToutSmsActivity
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.intents.IntentProvider
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService

/**
 * Created by piotr_000 on 05/03/2016.
 */
object NotificationHelper {

    private val TAG = "NotificationHelper"

    val EXTRA_ACTION_ICON = "$TAG.EXTRA_ACTION_ICON"
    val EXTRA_ACTION_TEXT = "$TAG.EXTRA_ACTION_TEXT"

    val EXTRA_TEXT = "$TAG.EXTRA_TEXT"
    val EXTRA_TITLE = "$TAG.EXTRA_TITLE"

    const val STOPPED_BY_STEP_COUNTER = 2
    const val HEADSET_PLUGGED_IN = 3
    const val SERVICE_STARTED_COMPLEX_ID = 4
    const val INCOMING_SMS = 5

    fun open(context: Context, id: Int, extras: Intent? = null) {
        var title: String? = null
        var text: String? = null
        var icon = R.mipmap.ic_launcher_score_white

        when (id) {
            SERVICE_STARTED_COMPLEX_ID -> {
                openComplex(context, id, icon)
                return
            }
            STOPPED_BY_STEP_COUNTER -> {
                title = context.resources.getString(R.string.service_notif_titre)
                text = context.resources.getString(R.string.stoppedByStepText)
                icon = R.drawable.ic_directions_run_24dp
            }
            HEADSET_PLUGGED_IN -> {
                title = context.resources.getString(R.string.service_notif_titre)
                text = context.resources.getString(R.string.headset_notification_text)
                icon = R.drawable.ic_headset_white_24dp
            }
            INCOMING_SMS -> {
                title = extras?.getStringExtra(EXTRA_TITLE)
                text = extras?.getStringExtra(EXTRA_TEXT)
            }
            else -> {
            }
        }
        open(context, id, icon, title, text, extras)
    }

    private fun getComplexNotificationView(context: Context, layout: Int): RemoteViews {
        // Using RemoteViews to bind custom layouts into Notification
        val notificationView = RemoteViews(
                context.packageName,
                layout
        )

        val intentSmsActivity = IntentProvider().provideNewSmsIntent(context)

        notificationView.setOnClickPendingIntent(R.id.complex_notification_btn_new_message,
                PendingIntent.getActivity(context, 0, intentSmsActivity, PendingIntent.FLAG_UPDATE_CURRENT))

        notificationView.setOnClickPendingIntent(R.id.complex_notification_btn_off,
                PendingIntent.getBroadcast(context, 0,
                        Intent(DisMoiToutSmsService.INTENT_DEACTIVATE_FROM_NOTIFICATION),
                        PendingIntent.FLAG_ONE_SHOT))

        return notificationView
    }

    private fun openComplex(context: Context, id: Int, icon: Int) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context,
                DisMoiToutSmsActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        val appName = context.getString(R.string.app_name)
        val mBuilder = NotificationCompat.Builder(context, appName)
                .setSmallIcon(icon)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setContent(getComplexNotificationView(context, R.layout.complex_notification))
                .setCustomBigContentView(getComplexNotificationView(context, R.layout.complex_notification_big))

        createChannel(notificationManager, appName, appName)

        notificationManager.notify(id, mBuilder.build())
    }

    private fun open(context: Context, id: Int, icon: Int, title: String?, text: String?, action: Intent?) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context,
                DisMoiToutSmsActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        val appName = context.getString(R.string.app_name)
        val mBuilder = NotificationCompat.Builder(context, appName)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, icon))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX)

        action?.let {
            val actionIcon = it.getIntExtra(EXTRA_ACTION_ICON, -1)
            val actionText = it.getStringExtra(EXTRA_ACTION_TEXT)
            if(actionIcon != -1 && actionText != null) {
                mBuilder.addAction(actionIcon, actionText, PendingIntent.getBroadcast(context, 0, it, 0))
            }
        }
        mBuilder.setContentIntent(pendingIntent)


        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(DisMoiToutSmsActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        val intent = Intent(context, DisMoiToutSmsActivity::class.java)
        stackBuilder.addNextIntent(intent)

        createChannel(notificationManager, appName, appName)

        notificationManager.notify(id, mBuilder.build())
    }

    fun close(context: Context, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }

    fun createChannel(notificationManager: NotificationManager, channelId: String, channelTitle: String) {
        /* Create or update. */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    channelTitle,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
