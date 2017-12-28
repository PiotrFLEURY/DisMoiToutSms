package fr.piotr.dismoitoutsms

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.os.Bundle
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
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.contacts.Contacts
import fr.piotr.dismoitoutsms.dialogs.ContactSelectionDialog
import fr.piotr.dismoitoutsms.fragments.MicrophoneFragment
import fr.piotr.dismoitoutsms.fragments.SmsSentFragment
import fr.piotr.dismoitoutsms.intents.IntentProvider
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.reception.SmsReceiver
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper
import fr.piotr.dismoitoutsms.util.*
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration
import fr.piotr.dismoitoutsms.util.Diction.*
import fr.piotr.dismoitoutsms.util.Instruction.*
import kotlinx.android.synthetic.main.smsrecudialog.*
import java.util.*

/**
 * @author Piotr
 */
class SmsRecuActivity : AbstractActivity() {

    private var microphoneFragment: MicrophoneFragment = MicrophoneFragment()

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
    //private var speechRecorder: MySpeechRecorder? = null
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


                ContactSelectionDialog.EVENT_CONTACT_SELECTED -> {
                    val contact = intent.getSerializableExtra(ContactSelectionDialog.EXTRA_CONTACT_SELECTED) as Contact
                    onContactSelected(contact)
                }

            }
        }
    }

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.action){
                EVENT_SMS_SENT -> {
                    when (resultCode) {
                        Activity.RESULT_OK -> onSmsSent()
                        else -> onSmsNotSent()
//                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(context, "Generic failure cause", Toast.LENGTH_SHORT).show()
//                        SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(context, "Service is currently unavailable", Toast.LENGTH_SHORT).show()
//                        SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(context, "No pdu provided", Toast.LENGTH_SHORT).show()
//                        SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(context, "Radio was explicitly turned off", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    enum class Parameters {
        DATE, CONTACT_NAME, CONTACT, MESSAGE, NUMERO_A_QUI_REPONDRE
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

        speech = TextToSpeechHelper(this) {onTtsInitialized()}

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
                        if(contact!=null && message!=null) {
                            SmsReceiver.getInstance().standBy(Message(date, contact!!, message!!))
                        }
                        finish()
                    }
                    else -> Log.i(TAG, "UNKNOWN_STATE: " + state)
                }
                lastState = state
            }
        }

    }

    private fun onTtsInitialized() {
        if(IntentProvider.TARGET_NEW_MESSAGE == intent.extras?.getString(IntentProvider.INTENT_TARGET)){
            askForContact()
        } else {
            if (ConfigurationManager.getBoolean(this, Configuration.PRIVATE_LIFE_MODE)) {
                speech.parler(String.format(format = getString(R.string.new_message_from), args = contactName), MESSAGE_RECU_MODE_VIE_PRIVEE)
            } else {
                onMessageRecu()
            }
        }
    }

    private fun onMessageRecu(){
        val text = contactName + " " + getString(R.string.dit) + " " + message
        speech.parler(text, MESSAGE_RECU)
    }

    private fun askForContact() {
        startSpeechRecognizer(Instruction.DICTER_CONTACT, getInstructionText(DICTER_CONTACT))
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
        filter.addAction(ContactSelectionDialog.EVENT_CONTACT_SELECTED)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        registerReceiver(smsSentReceiver, IntentFilter(EVENT_SMS_SENT))

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

        unregisterReceiver(smsSentReceiver)

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
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MicrophoneFragment.EVENT_DESTROY_SPEECH_RECOGNIZER))
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
            progress_sending.visibility = View.VISIBLE
            val pendingIntent = PendingIntent.getBroadcast(this, SMS_SENT_REQUEST_CODE, Intent(EVENT_SMS_SENT), 0)
            SmsManager.getDefault().sendTextMessage(numeroAQuiRepondre, null, reponse, pendingIntent, null)
        }
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
            showMicrophone(instruction, extraPrompt)
//            speech_instructions.text = extraPrompt
//            reponse_en_cours.text = ""
//            speechRecorder = MySpeechRecorder(this@SmsRecuActivity)
//            speechRecorder!!.startListening(instruction, extraPrompt)
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

    private fun onSpeechResult(instruction: Instruction, resultCode: Int, words: List<String>) {
        sablier.reset()
        if (Activity.RESULT_CANCELED == resultCode) {
            Snackbar.make(smsrecu_coordinator, R.string.error_occured, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.action_retry) { _ -> repondre() }
                    .show()
        } else if (instruction.`is`(LIRE_FERMER, REPONDRE_FERMER, MODIFIER_ENVOYER_FERMER) && resultCode == Activity.RESULT_OK) {

            when {
                instructionIs(words, getString(R.string.ajouter)) -> startSpeechRecognizer(AJOUTER, getString(R.string.reponse) + " " + reponse)
                instructionIs(words, getString(R.string.repondre), getString(R.string.modifier)) -> startSpeechRecognizer(REPONSE, getString(R.string.reponse))
                instructionIs(words, getString(R.string.envoyer)) -> {
                    envoyer()
                }
                instructionIs(words, getString(R.string.fermer)) -> LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(EVENT_BACK))
                instructionIs(words, getString(R.string.listen)) -> onMessageRecu()
                else -> {
                    startSpeechRecognizer(instruction, getInstructionText(instruction))
                }
            }
        } else if(instruction.`is`(AJOUTER) && resultCode == Activity.RESULT_OK) {
            reponse += " " + words[0]
            tv_reponse_message.text = reponse
            smsrecu_scrollview.fullScroll(View.FOCUS_DOWN)
            invalidateOptionsMenu()
            speech.parler(getString(R.string.votrereponseest) + reponse, VOUS_AVEZ_REPONDU)
        } else if (instruction.`is`(REPONSE) && resultCode == Activity.RESULT_OK) {
            reponse = words[0]
            tv_reponse_message.visibility = VISIBLE
            tv_reponse_message.text = reponse
            smsrecu_scrollview.fullScroll(View.FOCUS_DOWN)
            invalidateOptionsMenu()
            speech.parler(getString(R.string.votrereponseest) + reponse, VOUS_AVEZ_REPONDU)

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

    private fun getInstructionText(instruction: Instruction): String {
        return when(instruction){
            DICTER_CONTACT -> getString(R.string.dictate_contact_name)
            REPONSE -> getString(R.string.reponse)
            LIRE_FERMER -> {
                getString(R.string.dites) + " " + getString(R.string.listen) + " " + getString(R.string.ou) + " " + getString(R.string.fermer)
            }
            REPONDRE_FERMER -> {
                (getString(R.string.dites) + " " + getString(R.string.repondre)
                        + " " + getString(R.string.ou) + " " + getString(R.string.fermer))}
            MODIFIER_ENVOYER_FERMER -> {
                (getString(R.string.dites) + " " + getString(R.string.ajouter)
                        + ", " + getString(R.string.modifier)
                        + ", " + getString(R.string.envoyer) + " " + getString(R.string.ou) + " "
                        + getString(R.string.fermer))}
            AJOUTER -> getString(R.string.ajouter)
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
        startSpeechRecognizer(REPONSE, getInstructionText(REPONSE))
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
        supportFragmentManager.beginTransaction().remove(microphoneFragment).commit()
    }

    private fun showMicrophone(instruction: Instruction, extraPrompt: String) {
        microphoneFragment = MicrophoneFragment()
        var bundle = Bundle()
        bundle.putSerializable(MicrophoneFragment.INSTRUCTION, instruction)
        bundle.putString(MicrophoneFragment.EXTRA_PROMPT, extraPrompt)
        microphoneFragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(sms_recu_fragment_container.id, microphoneFragment, MicrophoneFragment.TAG).commit()
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

    fun onSmsSent() {
        progress_sending.visibility = View.INVISIBLE
        invalidateOptionsMenu()

        notifyMessageSent()
        addMessageToSent(numeroAQuiRepondre, reponse)
    }

    private fun notifyMessageSent() {
        val smsSentFragment = SmsSentFragment()
        val bundle = Bundle()
        bundle.putString(SmsSentFragment.EXTRA_REPONSE, reponse)
        smsSentFragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(sms_recu_fragment_container.id, smsSentFragment).commit()
    }

    fun onSmsNotSent() {
        progress_sending.visibility = View.INVISIBLE
        Snackbar.make(smsrecu_coordinator, R.string.error_occured, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_retry) { _ -> envoyer() }
                .show()
    }

    companion object {

        val TAG = "SmsRecuActivity"

        var EVENT_SMS_SENT = TAG + ".EVENT_SMS_SENT"

        val EXTRA_SPEECH_RESULT_CODE = TAG + ".EXTRA_SPEECH_RESULT_CODE"
        val EXTRA_SPEECH_INSTRUCTION = TAG + ".EXTRA_SPEECH_INSTRUCTION"
        val EVENT_SPEECH_RESULT = TAG + ".EVENT_SPEECH_RESULT"
        val EXTRA_SPEECH_WORDS = TAG + ".EXTRA_SPEECH_WORDS"
        val EVENT_HIDE_MICROPHONE = TAG + ".EVENT_HIDE_MICROPHONE"

        val EVENT_START_SPEECH_RECOGNIZER = TAG + ".EVENT_START_SPEECH_RECOGNIZER"
        val EVENT_FINISH = TAG + ".EVENT_FINISH"
        val EVENT_BACK = TAG + ".EVENT_BACK"

        val EXTRA_INSTRUCTION = TAG + ".EXTRA_INSTRUCTION"
        val EXTRA_PROMPT = TAG + ".EXTRA_PROMPT"

        private val TELEPHON_NUMBER_FIELD_NAME = "address"
        private val MESSAGE_BODY_FIELD_NAME = "body"
        private val SENT_MSGS_CONTET_PROVIDER = Uri.parse("content://sms/sent")

        val SMS_SENT_REQUEST_CODE = 1;
    }

}
