package fr.piotr.dismoitoutsms.dialogs

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.util.Instruction
import kotlinx.android.synthetic.main.new_speech_try.*

/**
 * Created by piotr on 11/03/2018.
 *
 */
class NewSpeechTryDialog : android.support.v4.app.DialogFragment() {

    companion object {
        const val TAG = "NewSpeechTryDialog"
        const val ARG_INSTRUCTION = "$TAG.ARG_INSTRUCTION"
        const val EVENT_NEXT = "$TAG.EVENT_NEXT"
        const val ARG_EVENT_NEXT = "$TAG.ARG_NEXT"
    }

    private val handler:Handler= Handler()
    private val callback:Runnable= Runnable{nextStep()}
    private val max = 3
    private var progress = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.new_speech_try, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        new_speech_try_progress.isIndeterminate = false
        new_speech_try_progress.max = max
        new_speech_try_progress.progress = 0
        scheduleNextStep()
    }

    private fun scheduleNextStep() {
        val countDownValue = "" + (max - progress)
        new_speech_try_text.text = getString(R.string.new_try_in, countDownValue)
        handler.postDelayed(callback, 1000)
    }

    private fun nextStep() {
        if(progress < 3){
            progress++
            scheduleNextStep()
        } else {
            dismiss()
            fireNext()
        }
    }

    private fun fireNext() {
        val intent = Intent(EVENT_NEXT)
        intent.putExtra(ARG_EVENT_NEXT, getInstructionArg())
        LocalBroadcastManager.getInstance(context!!).sendBroadcastSync(intent)
    }

    private fun getInstructionArg(): Instruction {
        return arguments?.getSerializable(ARG_INSTRUCTION) as Instruction
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(callback)
    }
}