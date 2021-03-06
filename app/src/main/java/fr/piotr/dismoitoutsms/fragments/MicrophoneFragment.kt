package fr.piotr.dismoitoutsms.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.speech.MySpeechRecorder
import fr.piotr.dismoitoutsms.util.Instruction
import fr.piotr.dismoitoutsms.util.setWakeUp
import kotlinx.android.synthetic.main.microphone.*

/**
 * Created by piotr on 23/12/2017.
 *
 */

class MicrophoneFragment : androidx.fragment.app.DialogFragment() {

    companion object {
        const val TAG = "MicrophoneFragment."
        const val EXTRA_PROMPT = "$TAG.EXTRA_PROMPT"
        const val INSTRUCTION = "$TAG.INSTRUCTION"

        const val EVENT_DESTROY_SPEECH_RECOGNIZER = "$TAG.EVENT_DESTROY_SPEECH_RECOGNIZER"
        const val EVENT_SPEECH_PARTIAL_RESULT = "$TAG.EVENT_SPEECH_PARTIAL_RESULT"

        const val EXTRA_SPEECH_WORDS = "$TAG.EXTRA_SPEECH_WORDS"

        const val EVENT_UPDATE_RESPONSE = "$TAG.EVENT_UPDATE_RESPONSE"
        const val EXTRA_UPDATE_RESPONSE_TEXT = "$TAG.EXTRA_UPDATE_RESPONSE_TEXT"
    }

    private lateinit var speechRecorder: MySpeechRecorder

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                EVENT_DESTROY_SPEECH_RECOGNIZER -> destroySpeechRecognizer()
                EVENT_SPEECH_PARTIAL_RESULT -> onPartialResult(intent.getStringArrayListExtra(EXTRA_SPEECH_WORDS))
                EVENT_UPDATE_RESPONSE -> updateResponseText(intent.getStringExtra(EXTRA_UPDATE_RESPONSE_TEXT))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.microphone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setWakeUp()

        val extraPrompt = arguments?.get(EXTRA_PROMPT) as String?
        val instruction = arguments?.get(INSTRUCTION) as Instruction?

        speech_instructions.text = extraPrompt
        reponse_en_cours.text = ""
        speechRecorder = MySpeechRecorder(activity)
        speechRecorder.startListening(instruction, extraPrompt)

    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(EVENT_DESTROY_SPEECH_RECOGNIZER)
        intentFilter.addAction(EVENT_SPEECH_PARTIAL_RESULT)
        intentFilter.addAction(EVENT_UPDATE_RESPONSE)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(activity!!).registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(receiver)
        destroySpeechRecognizer()
    }

    private fun destroySpeechRecognizer() {
        activity?.runOnUiThread { speechRecorder.destroy() }
    }

    private fun onPartialResult(words: List<String>) {
        //activity?.runOnUiThread {
        //    reponse_en_cours.text = words[0]
        //}
        val intent = Intent(EVENT_UPDATE_RESPONSE)
        intent.putExtra(EXTRA_UPDATE_RESPONSE_TEXT, words[0])
        context?.let { androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(it).sendBroadcast(intent) }
    }

    fun updateResponseText(text: String){
        reponse_en_cours.text = text
    }
}
