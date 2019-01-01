package fr.piotr.dismoitoutsms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.messages.ImagePart
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.messages.TextPart
import fr.piotr.dismoitoutsms.util.ContactHelper
import kotlinx.android.synthetic.main.activity_compose_sms.*
import java.util.*

class ComposeSmsActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ComposeSmsActivity"
        const val EXTRA_THREAD_ID = "$TAG.EXTRA_THREAD_ID"
        const val EVENT_SMS_SENT = "$TAG.EVENT_SMS_SENT"
        const val EXTRA_SMS_SENT = "$TAG.EXTRA_SMS_SENT"
        const val EVENT_MESSAGES_UPDATED = "$TAG.EVENT_MESSAGES_UPDATED"
        const val EXTRA_MESSAGES_UPDATED = "$TAG.EXTRA_MESSAGES_UPDATED"
        const val EVENT_SHOW_IMAGE_FULL_SCREEN = "$TAG.EVENT_SHOW_IMAGE_FULL_SCREEN"
        const val EXTRA_SHOW_IMAGE_FULL_SCREEN = "$TAG.EXTRA_SHOW_IMAGE_FULL_SCREEN"

        const val SMS_SENT_REQUEST_CODE = 1
    }

    class LoadThreadContentAsyncTask(private val localBroadcastManager: LocalBroadcastManager,
                                     private val threadId: Long) : AsyncTask<Context, Void, List<Message>>() {

        override fun doInBackground(vararg args: Context): List<Message> {
            val context = args[0]
            return MyMessagesManager.fetchThreadContent(context, threadId)
        }

        override fun onPostExecute(result: List<Message>) {
            super.onPostExecute(result)
            val intent = Intent(EVENT_MESSAGES_UPDATED)
            intent.putExtra(EXTRA_MESSAGES_UPDATED, result as ArrayList)
            localBroadcastManager.sendBroadcast(intent)
        }

    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun setAvatar(contact: Contact) {
            itemView.findViewById<CircleImageView>(R.id.cell_message_avatar)?.apply {
                if (contact.hasAPhoto()) {
                    setImageBitmap(ContactHelper.getPhotoContact(context, contact.photoId))
                } else {
                    //TODO
                }
            }
        }

        fun setContactName(contact: Contact) {
            itemView.findViewById<TextView>(R.id.cell_message_contact)?.apply {
                text = contact.name
            }
        }

        fun setText(message: Message) {
            itemView.findViewById<TextView>(R.id.cell_message_text)?.apply {
                text = if(message.discriminant == Message.Discriminant.SMS) {
                    message.message
                } else {
                    message.messageParts
                            .filter { it is TextPart }
                            .joinToString { (it as TextPart).text }
                }
                if (message.read) {
                    setTypeface(typeface, Typeface.NORMAL)
                } else {
                    setTypeface(typeface, Typeface.BOLD)
                }
                gravity = if(message.messageType == Message.MessageType.SENT) {
                    Gravity.END
                } else {
                    Gravity.START
                }
            }
        }

        fun setDate(date: String) {
            itemView.findViewById<TextView>(R.id.cell_message_date)?.apply {
                text = date
            }
        }

        fun setPhoto(message: Message) {
            itemView.findViewById<ImageView>(R.id.cell_message_photo)?.apply {
                setOnClickListener {}
                visibility = if(message.discriminant == Message.Discriminant.MMS) {
                    val images = message.messageParts.filter { it is ImagePart }
                    if (images.isNotEmpty()) {
                        val imagePart = images.first() as ImagePart
                        setImageBitmap(imagePart.image)
                        setOnClickListener {
                            val intent = Intent(EVENT_SHOW_IMAGE_FULL_SCREEN)
                            intent.putExtra(EXTRA_SHOW_IMAGE_FULL_SCREEN, imagePart.image)
                            LocalBroadcastManager.getInstance(itemView.context).sendBroadcast(intent)
                        }
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                } else {
                    View.GONE
                }
            }
        }
    }

    class HistoryAdapter(val context: Context, var messages: MutableList<Message> = mutableListOf()) : RecyclerView.Adapter<MessageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.cell_message_in, parent, false))
        }

        override fun getItemCount(): Int = messages.size

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            val contact = message.contact
            holder.setAvatar(contact)
            holder.setContactName(contact)
            holder.setText(message)
            when (message.messageType) {
                Message.MessageType.DRAFT -> holder.setDate(context.getString(R.string.message_not_sent))
                else -> holder.setDate(DateUtils.formatDateTime(context, message.date.time, DateUtils.FORMAT_SHOW_DATE.or(DateUtils.FORMAT_SHOW_TIME)))
            }
            holder.setPhoto(message)

        }

        fun updateData(messages: List<Message>) {
            this.messages.clear()
            this.messages.addAll(messages)
            this.messages.sortBy { it.date }
            notifyDataSetChanged()
        }

        fun addMessage(message: Message) {
            this.messages.add(message)
            notifyItemInserted(messages.size - 1)
        }

        fun remove(message: Message) {
            this.messages.remove(message)
            notifyItemRemoved(this.messages.size)
        }
    }

    private val historyAdapter = HistoryAdapter(this)

    private val threadId: Long
        get() = intent.getLongExtra(EXTRA_THREAD_ID, -1)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                EVENT_MESSAGES_UPDATED -> onMessagesUpdated(intent.getSerializableExtra(EXTRA_MESSAGES_UPDATED) as ArrayList<Message>)
                EVENT_SHOW_IMAGE_FULL_SCREEN -> showImageFullScreen(intent.getParcelableExtra(EXTRA_SHOW_IMAGE_FULL_SCREEN) as Bitmap)
            }
        }
    }

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
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

        Log.d(TAG, "Opening thread $threadId")

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        LoadThreadContentAsyncTask(LocalBroadcastManager.getInstance(this), threadId).execute(this)
    }

    private fun markAllAsRead(messages: List<Message>) {
        messages
                .filter { it.read.not() }
                .let {
                    MyMessagesManager.markAsRead(this, it)
                    historyAdapter.notifyDataSetChanged()
                }
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(smsSentReceiver, IntentFilter(EVENT_SMS_SENT))
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(EVENT_MESSAGES_UPDATED)
        intentFilter.addAction(EVENT_SHOW_IMAGE_FULL_SCREEN)
        localBroadcastManager.registerReceiver(receiver, intentFilter)

    }

    private fun onMessagesUpdated(messages: List<Message>) {
        historyAdapter.updateData(messages)
        rv_compose_history.scrollToPosition(historyAdapter.itemCount - 1)
        markAllAsRead(messages)

        title = messages.map { it.contact }.toSet().joinToString(separator = ", ") { it.name }

        iv_compose_send.setOnClickListener { sendMessage(messages.first { message -> message.messageType == Message.MessageType.INBOX }.contact.telephone) }
    }

    override fun onPause() {
        super.onPause()
        iv_compose_send.setOnClickListener {}

        unregisterReceiver(smsSentReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

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

    private fun changeDefaultSmsHandler() {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        startActivity(intent)
    }

    private fun sendMessage(phoneNumber: String) {
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            Snackbar.make(activity_compose_container, getString(R.string.not_yout_default_sms_handler), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.change_sms_manager_snack_action)) {changeDefaultSmsHandler()}
                    .show()
            return
        }

        val messageContent = et_compose.text.toString()
        if(messageContent.isEmpty()) {
            return
        }

        val message = MyMessagesManager.saveToDraft(this, messageContent, threadId)//FIXME MMS CASE

        val smsSentPendingIntent = Intent(EVENT_SMS_SENT)
        smsSentPendingIntent.putExtra(EXTRA_SMS_SENT, message)

        val pendingIntent = PendingIntent.getBroadcast(this, SMS_SENT_REQUEST_CODE, smsSentPendingIntent, 0)
        val smsManager = SmsManager.getDefault()
        val messages = smsManager.divideMessage(messageContent)
        if (messages.size == 1) {
            smsManager.sendTextMessage(phoneNumber, null, messages[0], pendingIntent, null)
        } else {
            smsManager.sendMultipartTextMessage(phoneNumber, null, messages, arrayListOf(pendingIntent), null)
        }

        historyAdapter.addMessage(message)
        rv_compose_history.scrollToPosition(historyAdapter.messages.size - 1)

        et_compose.text.clear()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(et_compose.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun onSmsSent(message: Message) {
        val messageSent = MyMessagesManager.transformDraftToSent(this, message)
        historyAdapter.remove(message)
        historyAdapter.addMessage(messageSent)
    }

    fun onSmsNotSent(message: Message, errorCode: Int) {
        message.errorCode = errorCode
        MyMessagesManager.setSendErrorCode(this, message)
    }

    fun showImageFullScreen(image: Bitmap) {
        iv_compose_full_screen.setImageBitmap(image)
        iv_compose_full_screen.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if(iv_compose_full_screen.visibility == View.VISIBLE) {
            iv_compose_full_screen.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    //TODO mark as unread
    //TODO add picture
    //TODO add emote
    //TODO start thread
    //TODO select contacts
    //TODO receive MMS
}
