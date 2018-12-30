package fr.piotr.dismoitoutsms.reception

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import fr.piotr.dismoitoutsms.SmsRecuActivity
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.util.ConfigurationManager
import fr.piotr.dismoitoutsms.util.ContactHelper
import java.util.*


/**
 * @author Piotr
 */
class SmsReceiver : BroadcastReceiver() {
    private val messagesEnAttente: SortedSet<Message>

    var isDictating: Boolean = false

    init {
        instance = this
        messagesEnAttente = TreeSet()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_RECEIVE_SMS == intent.action) {
            onSmsReceived(context, intent)
        }
    }

    private fun onSmsReceived(context: Context, intent: Intent) {
        Log.i("DisMoiToutSms", "SmsReceiver new message")
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>
            val format = bundle.getString("format")
            val messages = arrayOfNulls<SmsMessage>(pdus.size)
            for (i in pdus.indices) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                } else {
                    messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                }
            }
            val smsEntier = StringBuilder()
            for (sms in messages.filterNotNull()) {
                smsEntier.append(sms.messageBody)
            }
            val message = messages[0]
            message?.let {
                onSmsReceived(context, it.displayOriginatingAddress, smsEntier.toString())
            }
        }
    }

    fun onSmsReceived(context: Context, phoneNumber: String, smsEntier: String) {
        val contact = ContactHelper.getContact(context, phoneNumber)
        if (jePeuxDicterLeSmsDe(context, contact)) {
            Log.i("DisMoiToutSms", "SmsReceiver can speak")

            dicterLeSms(context, smsEntier, contact)
        }
    }

    private fun dicterLeSms(context: Context, contenuDuMessage: String, contact: Contact) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) {
            addToWaiting(contact, contenuDuMessage)
            telephonyManager.listen(object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, incomingNumber: String) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        Log.i("DisMoiToutSms", "SmsReceiver reading message")
                        nextMessage(context)
                        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE)
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE)
        } else {
            if (isDictating) {
                Log.i("DisMoiToutSms", "SmsReceiver waiting to speak")
                addToWaiting(contact, contenuDuMessage)
            } else if (!messagesEnAttente.isEmpty()) {
                Log.i("DisMoiToutSms",
                        "SmsReceiver appending message because of already waiting messages")
                addToWaiting(contact, contenuDuMessage)

                if (!isDictating) {
                    nextMessage(context)
                }
            } else {
                Log.i("DisMoiToutSms", "SmsReceiver reading message")
                afficherEtLiteLeMessage(context, Message(date = Calendar.getInstance().time, contact = contact, message = contenuDuMessage))
            }
        }
    }

    fun afficherEtLiteLeMessage(context: Context, message: Message) {

        val contenuDuMessage = message.message
        val phoneNumber = message.contact.telephone
        val contact = message.contact

        val contactName = contact.name

        val intent = Intent(context, SmsRecuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(SmsRecuActivity.Parameters.DATE.name, message.date.time)
        intent.putExtra(SmsRecuActivity.Parameters.CONTACT_NAME.toString(), contactName)
        intent.putExtra(SmsRecuActivity.Parameters.CONTACT.toString(), contact)
        intent.putExtra(SmsRecuActivity.Parameters.MESSAGE.toString(), contenuDuMessage)
        intent.putExtra(SmsRecuActivity.Parameters.NUMERO_A_QUI_REPONDRE.toString(), phoneNumber)
        context.startActivity(intent)

    }

    private fun addToWaiting(contact: Contact, phrase: String) {
        for (message in messagesEnAttente) {
            if (message.contactIs(contact)) {
                message.append(phrase)
                return
            }
        }
        messagesEnAttente.add(Message(date = Calendar.getInstance().time, contact = contact, message = phrase))
    }

    fun standBy(message: Message) {
        messagesEnAttente.add(message)
    }

    private fun jePeuxDicterLeSmsDe(context: Context, contact: Contact?): Boolean {
        val uniquementContacts = ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS)
        return !(uniquementContacts && (contact == null || ConfigurationManager.leContactEstBannis(context, contact.id)))
    }

    fun nextMessage(context: Context) {
        if (!messagesEnAttente.isEmpty()) {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    if (!messagesEnAttente.isEmpty()) {
                        if (isDictating) {
                            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(SmsRecuActivity.EVENT_FINISH))
                        }
                        val message = messagesEnAttente.first()
                        messagesEnAttente.remove(message)
                        afficherEtLiteLeMessage(context, message)
                    }
                }
            }, 2000)
        }
    }

    companion object {

        const val ACTION_RECEIVE_SMS = "android.provider.Telephony.SMS_RECEIVED"

        private lateinit var instance: SmsReceiver

        fun getInstance(): SmsReceiver {
            return instance
        }
    }


}
