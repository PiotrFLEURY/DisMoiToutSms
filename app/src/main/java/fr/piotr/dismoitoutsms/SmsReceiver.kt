package fr.piotr.dismoitoutsms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.util.NotificationHelper

class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "SmsReceiver"
        // Key for the string that's delivered in the action's intent.
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Received new SMS $intent")

        val (phoneNumber, body) = getSmsContent(intent)

        context?.let {
//            NotificationHelper.open(it, NotificationHelper.INCOMING_SMS, extras)
            val threadId = Telephony.Threads.getOrCreateThreadId(context, phoneNumber)
            val message = MyMessagesManager.saveToInbox(context, phoneNumber, body, threadId)
            notifySmsReceived(context, message)
        }
    }

    private fun notifySmsReceived(context: Context, message: Message) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, ComposeSmsActivity::class.java)
        intent.putExtra(ComposeSmsActivity.EXTRA_THREAD_ID, message.threadId)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val appName = context.getString(R.string.app_name)
        val mBuilder = NotificationCompat.Builder(context, appName)
                .setContentTitle(message.contact.telephone)
                .setContentText(message.message)
                .setSmallIcon(R.mipmap.ic_launcher_score)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_score))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(message.message))
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX)

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mBuilder.addAction(buildReplyAction(context, message.contact.telephone, message.threadId))
        }

        mBuilder.addAction(R.drawable.ic_message_24dp, context.getString(R.string.mark_as_read),
                PendingIntent.getService(context, 1, getMarkAsReadIntent(context, message), 0))

        mBuilder.setContentIntent(pendingIntent)


        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ThreadsActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack

        stackBuilder.addNextIntent(intent)

        NotificationHelper.createChannel(notificationManager, "SMS", "SMS")

        notificationManager.notify(message.threadId.toInt(), mBuilder.build())
    }

    private fun getMarkAsReadIntent(context: Context, message: Message): Intent {
        val markAsReadIntent = Intent(context, HeadlessSmsSendService::class.java)
        markAsReadIntent.action = HeadlessSmsSendService.MARK_AS_READ_FROM_NOTIFICATION
        markAsReadIntent.putExtra(HeadlessSmsSendService.MARK_AS_READ_FROM_NOTIFICATION_EXTRA_MESSAGE, message)
        return markAsReadIntent
    }

    private fun buildReplyAction(context: Context, phoneNumber: String, threadId: Long): NotificationCompat.Action {

        val replyLabel: String = context.getString(R.string.reply_label)
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(replyLabel)
            build()
        }

        // Build a PendingIntent for the reply action to trigger.
        val replyPendingIntent: PendingIntent =
                PendingIntent.getService(context,
                        threadId.toInt(),
                        getMessageReplyIntent(context, phoneNumber),
                        PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the reply action and add the remote input.
        return NotificationCompat.Action.Builder(R.drawable.ic_send_24dp,
                        context.getString(R.string.reply_label), replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build()
    }

    private fun getMessageReplyIntent(context: Context, phoneNumber: String): Intent {
        val intent = Intent(context, HeadlessSmsSendService::class.java)
        intent.action = "android.intent.action.RESPOND_VIA_MESSAGE"
        intent.data = Uri.fromParts("smsto", phoneNumber, null)
        return intent
    }

    private fun getSmsContent(intent: Intent?): Array<String> {
        val extras = intent?.extras
        extras?.let { bundle ->
            val pdus = bundle.get("pdus") as Array<*>
            val format = bundle.getString("format")

            val messages = pdus.mapNotNull {
                createFromPdu(it as ByteArray, format)
            }

            if (messages.size > -1) {
                val body = messages.joinToString { it.messageBody }
                val phoneNumber = messages.first().displayOriginatingAddress ?: "unknown"

                return arrayOf(phoneNumber, body)
            }
        }
        throw IllegalArgumentException("intent does not contain message data")
    }

    private fun createFromPdu(pdu: ByteArray, format: String?): SmsMessage? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SmsMessage.createFromPdu(pdu, format)
        } else {
            SmsMessage.createFromPdu(pdu)
        }
    }
}