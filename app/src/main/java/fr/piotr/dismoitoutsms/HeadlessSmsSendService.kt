package fr.piotr.dismoitoutsms

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.annotation.Nullable
import androidx.core.app.RemoteInput
import fr.piotr.dismoitoutsms.SmsReceiver.Companion.KEY_TEXT_REPLY
import fr.piotr.dismoitoutsms.messages.Message


class HeadlessSmsSendService : IntentService(".HeadlessSmsSendService") {

    companion object {
        const val TAG = "HeadlessSmsSendService"
        const val EVENT_SMS_SENT = "$TAG.EVENT_SMS_SENT"
        const val EXTRA_SMS_SENT = "$TAG.EXTRA_SMS_SENT"

        const val SMS_SENT_REQUEST_CODE = 1

        const val REPLY_FROM_NOTIFICATION = "$TAG.REPLY_FROM_NOTIFICATION"
        const val REPLY_FROM_NOTIFICATION_EXTRA_PHONE_NUMBER = "$TAG.REPLY_FROM_NOTIFICATION_EXTRA_PHONE_NUMBER"
        const val MARK_AS_READ_FROM_NOTIFICATION = "$TAG.MARK_AS_READ_FROM_NOTIFICATION"
        const val MARK_AS_READ_FROM_NOTIFICATION_EXTRA_MESSAGE = "$TAG.MARK_AS_READ_FROM_NOTIFICATION_EXTRA_MESSAGE"
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onHandleIntent(intent: Intent) {
        when(intent.action) {
            TelephonyManager.ACTION_RESPOND_VIA_MESSAGE -> {
                val extras = intent.extras ?: return

                val message = extras.getString(Intent.EXTRA_TEXT)
                val intentUri = intent.data
                val recipients = getRecipients(intentUri)

                if (TextUtils.isEmpty(recipients)) {
                    return
                }

                if (TextUtils.isEmpty(message)) {
                    return
                }

                val destinations = TextUtils.split(recipients, ";").toSet()

                sendMessage(destinations, message)
            }
            REPLY_FROM_NOTIFICATION -> {
                val phoneNumber = intent.getStringExtra(REPLY_FROM_NOTIFICATION_EXTRA_PHONE_NUMBER)
                val message = getMessageText(intent)
                message?.let {
                    sendMessage(setOf(phoneNumber), message)
                }
            }
            MARK_AS_READ_FROM_NOTIFICATION -> {
                val message = intent.getSerializableExtra(MARK_AS_READ_FROM_NOTIFICATION_EXTRA_MESSAGE) as Message
                MyMessagesManager.markAsRead(this, listOf(message))
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(message.threadId.toInt())
            }
        }
    }

    private fun getMessageText(intent: Intent): String? {
        return RemoteInput.getResultsFromIntent(intent).getString(KEY_TEXT_REPLY)
    }

    private fun getRecipients(uri: Uri): String {
        val base = uri.schemeSpecificPart
        val pos = base.indexOf('?')
        return if (pos == -1) base else base.substring(0, pos)
    }

    private fun sendMessage(destinations: Set<String>, message: String) {

        val threadId = Telephony.Threads.getOrCreateThreadId(this, destinations)
        for (destination in destinations) {

            val values = ContentValues()
            values.put(Telephony.Sms.Sent.ADDRESS, destination)
            values.put(Telephony.Sms.Sent.BODY, message)

            MyMessagesManager.saveToDraft(this, message, threadId)//FIXME MMS CASE

            val smsSentPendingIntent = Intent(EVENT_SMS_SENT)
            smsSentPendingIntent.putExtra(EXTRA_SMS_SENT, message)

            val pendingIntent = PendingIntent.getBroadcast(this, SMS_SENT_REQUEST_CODE, smsSentPendingIntent, 0)
            val smsManager = SmsManager.getDefault()
            val messages = smsManager.divideMessage(message)
            if (messages.size == 1) {
                smsManager.sendTextMessage(destination, null, messages[0], pendingIntent, null)
            } else {
                smsManager.sendMultipartTextMessage(destination, null, messages, arrayListOf(pendingIntent), null)
            }
        }
    }
}
