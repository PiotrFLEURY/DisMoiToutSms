package fr.piotr.dismoitoutsms.fragments

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.util.AnimUtils
import kotlinx.android.synthetic.main.sms_sent.*

/**
 * Created by piotr on 23/12/2017.
 *
 */
class SmsSentFragment: androidx.fragment.app.DialogFragment() {

    companion object {
        const val TAG = "SmsSentFragment"
        const val EXTRA_REPONSE = "$TAG.EXTRA_REPONSE"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sms_sent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sms_sent_tv_content.text = arguments?.getString(EXTRA_REPONSE)

    }

    override fun onResume() {
        super.onResume()
        sms_sent_tv_content.post({start()})
    }

    override fun onPause() {
        super.onPause()
        sms_sent_tv_content.handler?.removeCallbacksAndMessages(null)
    }

    private fun start() {
        if(isDetached){
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimUtils.reveal(sms_sent_card)
        } else {
            sms_sent_card.visibility = View.VISIBLE
        }

        sms_sent_tv_content.postDelayed({ end() }, 3000)
    }

    fun end() {
        if(isDetached){
            return
        }
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        activity?.moveTaskToBack(true)
        activity?.finish()
    }

}