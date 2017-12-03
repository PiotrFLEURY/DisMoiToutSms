package fr.piotr.dismoitoutsms

import android.app.Activity
import android.content.*
import android.net.Uri
import android.speech.RecognizerIntent
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.telephony.PhoneStateListener
import android.telephony.PhoneStateListener.LISTEN_CALL_STATE
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.Toast
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.contacts.Contacts
import fr.piotr.dismoitoutsms.dialogs.ContactSelectionDialog
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.reception.SmsReceiver
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper
import fr.piotr.dismoitoutsms.speech.MySpeechRecorder
import fr.piotr.dismoitoutsms.util.*
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration
import fr.piotr.dismoitoutsms.util.Diction.*
import fr.piotr.dismoitoutsms.util.Instruction.*
import kotlinx.android.synthetic.main.microphone.*
import kotlinx.android.synthetic.main.smsrecudialog.*
import java.util.*

/**
 * @author Piotr
 */
class SmsRecuActivity : AbstractActivity() {

    private lateinit var date: Date
    private lateinit var contactName: String
    private var contact: Contact? = null
    private var message: String? = null

    private val isReconnaissanceInstallee: Boolean
        get() {
            return !packageManager.queryIntentActivities(Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isEmpty()
        }

    private var reponse: String? = null
    private var numeroAQuiRepondre: String? = null
    private lateinit var sablier: Sablier
    private lateinit var speech: TextToSpeechHelper
    private var speechRecorder: MySpeechRecorder? = null
    private lateinit var phoneStateListener: PhoneStateListener

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                EVENT_BACK -> onBackPressed()
                EVENT_FINISH -> finish()
                EVENT_START_SPEECH_RECOGNIZER -> startSpeechRecognizer(intent.getSerializableExtra(EXTRA_INSTRUCTION) as Instruction,
                        intent.getStringExtra(EXTRA_PROMPT))
                EVENT_HIDE_MICROPHONE -> hideMicrophone()
                EVENT_SPEECH_RESULT -> onSpeechResult(intent.getSerializableExtra(EXTRA_SPEECH_INSTRUCTION) as Instruction,
                        intent.getIntExtra(EXTRA_SPEECH_RESULT_CODE, -1),
                        intent.getStringArrayListExtra(EXTRA_SPEECH_WORDS))
                EVENT_SPEECH_PARTIAL_RESULT -> onPartialResult(intent.getStringArrayListExtra(EXTRA_SPEECH_WORDS))
                EVENT_DESTROY_SPEECH_RECOGNIZER -> destroySpeechRecognizer()
                ContactSelectionDialog.EVENT_CONTACT_SELECTED -> {
                    val contact = intent.getSerializableExtra(ContactSelectionDialog.EXTRA_CONTACT_SELECTED) as Contact
                    onContactSelected(contact)
                }
            }
        }
    }

    enum class Parameters {
        DATE, CONTACT_NAME, CONTACT, MESSAGE, NUMERO_A_QUI_REPONDRE
    }

    private fun destroySpeechRecognizer() {
        runOnUiThread { speechRecorder?.destroy() }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        WakeLockManager.setWakeUp(this)

        // Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        SmsReceiver.getInstance().isDictating = true

        val extras = intent.extras
        date = Date(extras!!.getLong(Parameters.DATE.name))
        contactName = extras.getString(Parameters.CONTACT_NAME.toString())
        contact = extras.getSerializable(Parameters.CONTACT.toString()) as Contact
        message = extras.getString(Parameters.MESSAGE.toString())
        numeroAQuiRepondre = extras.getString(Parameters.NUMERO_A_QUI_REPONDRE.toString())
        setContentView(R.layout.smsrecudialog)

        initPhoto()

        title = contactName

        initExpediteur()
        initMessage()
        initReponse()

        sablier = Sablier()
        sablier.start()

        speech = TextToSpeechHelper(this) {
            if (message != null) {

                if(ConfigurationManager.getBoolean(this, Configuration.PRIVATE_LIFE_MODE)) {
                    speech.parler(String.format(format = getString(R.string.new_message_from), args = contactName), MESSAGE_RECU_MODE_VIE_PRIVEE)
                } else {
                    onMessageRecu()
                }

            } else {
                askForContact()
            }
        }

        phoneStateListener = object : PhoneStateListener() {

            internal var lastState = -1

            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                super.onCallStateChanged(state, incomingNumber)
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                    }
                    TelephonyManager.CALL_STATE_RINGING,
                        // Le téléphone sonne
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        SmsReceiver.getInstance().standBy(Message(date, contact!!, message!!))
                        finish()
                    }
                    else -> Log.i(TAG, "UNKNOWN_STATE: " + state)
                }
                lastState = state
            }
        }


    }

    private fun onMessageRecu(){
        val text = contactName + " " + getString(R.string.dit) + " " + message
        speech.parler(text, MESSAGE_RECU)
    }

    private fun askForContact() {
        startSpeechRecognizer(Instruction.DICTER_CONTACT, getString(R.string.dictate_contact_name))
    }

    private fun initPhoto() {
        if (contact != null && contact!!.hasAPhoto()) {
            smsrecucontactphoto.setImageBitmap(ContactHelper.getPhotoContact(this, contact!!.photoId))
        }
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter()
        filter.addAction(EVENT_FINISH)
        filter.addAction(EVENT_START_SPEECH_RECOGNIZER)
        filter.addAction(EVENT_BACK)
        filter.addAction(EVENT_HIDE_MICROPHONE)
        filter.addAction(EVENT_SPEECH_RESULT)
        filter.addAction(EVENT_SPEECH_PARTIAL_RESULT)
        filter.addAction(EVENT_DESTROY_SPEECH_RECOGNIZER)
        filter.addAction(ContactSelectionDialog.EVENT_CONTACT_SELECTED)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        SmsReceiver.getInstance().isDictating = true

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, LISTEN_CALL_STATE)

    }

    private fun repondre() {
        startSpeechRecognizer(REPONSE, getString(R.string.reponse))
        sablier.reset()
    }

    private fun stop() {
        speech.stopLecture()
        sablier.reset()
    }

    private fun repeter() {
        if (message != null) {
            speech.parler(message, MESSAGE_RECU)
            sablier.reset()
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

        SmsReceiver.getInstance().isDictating = false

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

    }

    override fun finish() {
        super.finish()
        end()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun end() {
        SmsReceiver.getInstance().isDictating = false
        speech.stopLecture()
        speech.shutdown()
        sablier.finished()
        if (speechRecorder != null) {
            destroySpeechRecognizer()
        }
        SmsReceiver.getInstance().nextMessage(this)
    }

    private fun initMessage() {
        val tvMessage = text(R.id.message)
        if (message == null) {
            tvMessage.visibility = GONE
        } else {
            tvMessage.text = message
        }
    }

    private fun initReponse() {
        tv_reponse_message.visibility = INVISIBLE
    }

    private fun initExpediteur() {
        if (contact != null) {
            tv_contact_name.text = contact!!.name
        }
    }

    private fun envoyer() {
        if (!TextUtils.isEmpty(numeroAQuiRepondre) && !TextUtils.isEmpty(reponse)) {
            SmsManager.getDefault().sendTextMessage(numeroAQuiRepondre, null, reponse, null, null)
            tv_reponse_message.text = getString(R.string.messageenvoye)
            invalidateOptionsMenu()
            Toast.makeText(this@SmsRecuActivity,
                    getString(R.string.vousavezrepondu) + " " + reponse, Toast.LENGTH_LONG).show()
            addMessageToSent(numeroAQuiRepondre, reponse)
        }
        finish()
        moveTaskToBack(true)
    }

    /**
     * Permet d'ajouter le SMS envoyé dans la conversation
     *
     */
    private fun addMessageToSent(telNumber: String?, messageBody: String?) {
        val sentSms = ContentValues()
        sentSms.put(TELEPHON_NUMBER_FIELD_NAME, telNumber)
        sentSms.put(MESSAGE_BODY_FIELD_NAME, messageBody)

        val contentResolver = contentResolver
        contentResolver.insert(SENT_MSGS_CONTET_PROVIDER, sentSms)
    }

    private fun startSpeechRecognizer(instruction: Instruction, extraPrompt: String) {
        sablier.reset()

        runOnUiThread {
            showMicrophone()
            speech_instructions.text = extraPrompt
            reponse_en_cours.text = ""
            speechRecorder = MySpeechRecorder(this@SmsRecuActivity)
            speechRecorder!!.startListening(instruction, extraPrompt)
        }
    }

    private fun instructionIs(instructions: List<String>, vararg possibilities: String): Boolean {
        for (instruction in instructions) {
            val lowerInstruction = instruction.toLowerCase()
            for (possibility in possibilities) {
                if (lowerInstruction.contains(possibility.toLowerCase())) {
                    return true
                }
            }
        }
        return false
    }

    private fun onPartialResult(words: List<String>) {
        runOnUiThread {
            reponse_en_cours.text = words[0]
        }
    }

    private fun onSpeechResult(instruction: Instruction, resultCode: Int, words: List<String>) {
        sablier.reset()
        if (Activity.RESULT_CANCELED == resultCode) {
            Snackbar.make(smsrecu_coordinator, R.string.error_occured, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.action_retry) { v -> repondre() }
                    .show()
        } else if (instruction.`is`(LIRE_FERMER, REPONDRE_FERMER, MODIFIER_ENVOYER_FERMER) && resultCode == Activity.RESULT_OK) {

            when {
                instructionIs(words, getString(R.string.repondre), getString(R.string.modifier)) -> startSpeechRecognizer(REPONSE, getString(R.string.reponse))
                instructionIs(words, getString(R.string.envoyer)) -> {
                    envoyer()
                    speech.parler(getString(R.string.messageenvoye), MESSAGE_ENVOYE)
                }
                instructionIs(words, getString(R.string.fermer)) -> LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(EVENT_BACK))
                instructionIs(words, getString(R.string.listen)) -> onMessageRecu()
            }

        } else if (instruction.`is`(REPONSE) && resultCode == Activity.RESULT_OK) {
            reponse = words[0]
            tv_reponse_message.visibility = VISIBLE
            tv_reponse_message.text = reponse
            smsrecu_scrollview.fullScroll(View.FOCUS_DOWN)
            invalidateOptionsMenu()
            speech.parler(getString(R.string.votrereponseest) + reponse!!, VOUS_AVEZ_REPONDU)

        } else if (instruction.`is`(DICTER_CONTACT)) {
            if (resultCode == Activity.RESULT_OK) {
                val result = words[0]
                val correspondances = getCorrespondance(result)
                val contactSelectionDialog = ContactSelectionDialog(this)
                contactSelectionDialog.setContacts(correspondances)
                contactSelectionDialog.show()
            } else {
                onBackPressed()
            }
        }
    }

    private fun onContactSelected(contact: Contact) {
        listenForNewMessage(contact)
    }

    private fun listenForNewMessage(contact: Contact) {
        this.contact = contact
        contactName = this.contact!!.name
        numeroAQuiRepondre = this.contact!!.telephone
        initPhoto()
        title = contactName
        initExpediteur()
        startSpeechRecognizer(REPONSE, getString(R.string.reponse))
    }

    private fun getCorrespondance(result: String): Contacts {
        val correspondances = Contacts()
        for (aContact in ContactHelper.getAllContacts()) {
            if (aContact.name.contains(result)) {
                correspondances.add(aContact)
            }
        }
        return correspondances
    }

    private fun hideMicrophone() {
        microphone.visibility = GONE
    }

    private fun showMicrophone() {
        microphone.visibility = VISIBLE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        SmsReceiver.getInstance().isDictating = false
        sablier.finished()
    }

    override fun onDestroy() {
        SmsReceiver.getInstance().isDictating = false
        sablier.finished()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.sms_recu_menu, menu)

        menu.findItem(R.id.action_answer).isEnabled = isReconnaissanceInstallee && ConfigurationManager.getBoolean(this@SmsRecuActivity,
                Configuration.COMMANDE_VOCALE)

        menu.findItem(R.id.action_send).isEnabled = (reponse != null && !reponse!!.isEmpty()
                && numeroAQuiRepondre != null && !numeroAQuiRepondre!!.isEmpty())

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_repeat -> repeter()
            R.id.action_stop -> stop()
            R.id.action_answer -> repondre()
            R.id.action_send -> envoyer()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        val TAG = "SmsRecuActivity"

        val EXTRA_SPEECH_WORDS = TAG + ".EXTRA_SPEECH_WORDS"
        val EXTRA_SPEECH_RESULT_CODE = TAG + ".EXTRA_SPEECH_RESULT_CODE"
        val EXTRA_SPEECH_INSTRUCTION = TAG + ".EXTRA_SPEECH_INSTRUCTION"
        val EVENT_SPEECH_RESULT = TAG + ".EVENT_SPEECH_RESULT"

        val EVENT_SPEECH_PARTIAL_RESULT = TAG + ".EVENT_SPEECH_PARTIAL_RESULT"

        val EVENT_DESTROY_SPEECH_RECOGNIZER = TAG + ".EVENT_DESTROY_SPEECH_RECOGNIZER"

        val EVENT_HIDE_MICROPHONE = TAG + ".EVENT_HIDE_MICROPHONE"

        val EVENT_START_SPEECH_RECOGNIZER = TAG + ".EVENT_START_SPEECH_RECOGNIZER"
        val EVENT_FINISH = TAG + ".EVENT_FINISH"
        val EVENT_BACK = TAG + ".EVENT_BACK"

        val EXTRA_INSTRUCTION = TAG + ".EXTRA_INSTRUCTION"
        val EXTRA_PROMPT = TAG + ".EXTRA_PROMPT"

        private val TELEPHON_NUMBER_FIELD_NAME = "address"
        private val MESSAGE_BODY_FIELD_NAME = "body"
        private val SENT_MSGS_CONTET_PROVIDER = Uri.parse("content://sms/sent")
    }

}
