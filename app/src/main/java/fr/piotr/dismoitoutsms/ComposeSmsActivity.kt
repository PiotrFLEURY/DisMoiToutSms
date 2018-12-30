package fr.piotr.dismoitoutsms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.telephony.SmsManager
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.messages.Thread
import fr.piotr.dismoitoutsms.util.ContactHelper
import kotlinx.android.synthetic.main.activity_compose_sms.*

class ComposeSmsActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ComposeSmsActivity"
        const val EXTRA_THREAD = "$TAG.EXTRA_THREAD"
        const val EVENT_SMS_SENT = "$TAG.EVENT_SMS_SENT"
        const val EXTRA_SMS_SENT = "$TAG.EXTRA_SMS_SENT"

        const val SMS_SENT_REQUEST_CODE = 1
    }

    class MessageViewHolder(view: View): RecyclerView.ViewHolder(view) {

        fun setAvatar(contact: Contact) {
            itemView.findViewById<CircleImageView>(R.id.cell_message_avatar)?.apply {
                if (contact.hasAPhoto()) {
                    setImageBitmap(ContactHelper.getPhotoContact(context, contact.photoId))
                } else {

                }
            }
        }

        fun setText(message: Message) {
            itemView.findViewById<TextView>(R.id.cell_message_text)?.apply {
                text = message.message
                if(message.read) {
                    setTypeface(typeface, Typeface.NORMAL)
                } else {
                    setTypeface(typeface, Typeface.BOLD)
                }
            }
        }

        fun setDate(date: String) {
            itemView.findViewById<TextView>(R.id.cell_message_date)?.apply {
                text = date
            }
        }
    }

    class HistoryAdapter(val context: Context, var messages: MutableList<Message> = mutableListOf()): RecyclerView.Adapter<MessageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.cell_message_in, parent, false))
        }

        override fun getItemCount(): Int = messages.size

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            holder.setAvatar(message.contact)
            holder.setText(message)
            holder.setDate(DateUtils.formatDateTime(context, message.date.time, DateUtils.FORMAT_SHOW_TIME))
        }

        fun updateData(messages: List<Message>) {
            this.messages.clear()
            this.messages.addAll(messages)
            notifyDataSetChanged()
        }

        fun addMessage(message: Message) {
            this.messages.add(message)
            notifyItemInserted(messages.size-1)
        }
    }

    private val historyAdapter = HistoryAdapter(this)

    lateinit var contact: Contact
    lateinit var messages: List<Message>

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.action){
                EVENT_SMS_SENT -> {
                    val message = intent.getSerializableExtra(EXTRA_SMS_SENT) as Message
                    when (resultCode) {
                        Activity.RESULT_OK -> onSmsSent(message)
                        else -> onSmsNotSent(message, resultCode)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_sms)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        rv_compose_history.hasFixedSize()
        rv_compose_history.layoutManager = LinearLayoutManager(this)
        rv_compose_history.adapter = historyAdapter

        getThread().let {thread ->
            Log.d(TAG, "Opening thread with ${thread.messageCount} messages")
            title = contact.name
        }

        contact = ContactHelper.getContactById(this, getThread().recipientIds.first())//FIXME get all recipients
        messages = MyMessagesManager.fetchThreadContent(this, getThread().id)
    }

    override fun onResume() {
        super.onResume()
        historyAdapter.updateData(messages)
        iv_compose_send.setOnClickListener { sendMessage() }

        registerReceiver(smsSentReceiver, IntentFilter(EVENT_SMS_SENT))
    }

    override fun onPause() {
        super.onPause()
        iv_compose_send.setOnClickListener {}

        unregisterReceiver(smsSentReceiver)
    }

    private fun getThread() = intent.getSerializableExtra(EXTRA_THREAD) as Thread

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sendMessage() {
        val phoneNumber = contact.telephone
        val messageContent = et_compose.text.toString()

        val message = MyMessagesManager.saveToSent(this, phoneNumber, messageContent)

        val smsSentPendingIntent = Intent(EVENT_SMS_SENT)
        smsSentPendingIntent.putExtra(EXTRA_SMS_SENT, message)

        val pendingIntent = PendingIntent.getBroadcast(this, SMS_SENT_REQUEST_CODE, smsSentPendingIntent, 0)
        val smsManager = SmsManager.getDefault()
        val messages = smsManager.divideMessage(messageContent)
        if(messages.size==1) {
            smsManager.sendTextMessage(phoneNumber, null, messages[0], pendingIntent, null)
        } else {
            smsManager.sendMultipartTextMessage(phoneNumber, null, messages, arrayListOf(pendingIntent), null)
        }

        historyAdapter.addMessage(message)
        rv_compose_history.scrollToPosition(historyAdapter.messages.size-1)

        et_compose.text.clear()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(et_compose.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun onSmsSent(message: Message) {
        MyMessagesManager.setSendDate(this, message)
    }

    fun onSmsNotSent(message: Message, errorCode: Int) {
        message.errorCode = errorCode
        MyMessagesManager.setSendErrorCode(this, message)
    }

    //TODO mark as read
    //TODO mark all as read
    //TODO mark as unread
    //TODO send message
    //TODO add picture
    //TODO add emote
}
