package fr.piotr.dismoitoutsms.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.speech.MySpeechRecorder
import fr.piotr.dismoitoutsms.util.Instruction
import kotlinx.android.synthetic.main.microphone.*

/**
 * Created by piotr on 23/12/2017.
 *
 */

class MicrophoneFragment : Fragment() {

    companion object {
        val TAG = "MicrophoneFragment."
        val EXTRA_PROMPT = TAG + "EXTRA_PROMPT"
        val INSTRUCTION = TAG + "INSTRUCTION"

        val EVENT_DESTROY_SPEECH_RECOGNIZER = TAG + ".EVENT_DESTROY_SPEECH_RECOGNIZER"
        val EVENT_SPEECH_PARTIAL_RESULT = TAG + ".EVENT_SPEECH_PARTIAL_RESULT"

        val EXTRA_SPEECH_WORDS = TAG + ".EXTRA_SPEECH_WORDS"
    }

    private lateinit var speechRecorder: MySpeechRecorder

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                EVENT_DESTROY_SPEECH_RECOGNIZER -> destroySpeechRecognizer()
                EVENT_SPEECH_PARTIAL_RESULT -> onPartialResult(intent.getStringArrayListExtra(EXTRA_SPEECH_WORDS))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.microphone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var extraPrompt = arguments?.get(EXTRA_PROMPT) as String?
        var instruction = arguments?.get(INSTRUCTION) as Instruction?

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
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(receiver)
        destroySpeechRecognizer()
    }

    private fun destroySpeechRecognizer() {
        activity?.runOnUiThread { speechRecorder.destroy() }
    }

    private fun onPartialResult(words: List<String>) {
        activity?.runOnUiThread {
            reponse_en_cours.text = words[0]
        }
    }
}