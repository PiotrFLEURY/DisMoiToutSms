package fr.piotr.dismoitoutsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import fr.piotr.dismoitoutsms.util.NotificationHelper
import java.lang.IllegalArgumentException

class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Received new SMS $intent")

        val (phoneNumber, body) = getSmsContent(intent)
        Log.d(TAG, "Phone number is $phoneNumber")
        Log.d(TAG, "Body is $body")

        val extras = Intent().apply {
            putExtra(NotificationHelper.EXTRA_TITLE, phoneNumber)
            putExtra(NotificationHelper.EXTRA_TEXT, body)
        }
        context?.let {
            NotificationHelper.open(it, NotificationHelper.INCOMING_SMS, extras)
            MyMessagesManager.saveToInbox(context, phoneNumber, body)
        }
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