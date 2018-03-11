package fr.piotr.dismoitoutsms.fragments

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.util.AnimUtils

/**
 * Created by piotr on 23/12/2017.
 *
 */
class SmsSentFragment: Fragment() {

    companion object {
        val TAG = "SmsSentFragment"
        val EXTRA_REPONSE = TAG + ".EXTRA_REPONSE"
    }

    lateinit var tvContent: TextView
    lateinit var card: CardView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sms_sent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        card = activity!!.findViewById<View>(R.id.sms_sent_card) as CardView
        tvContent = activity!!.findViewById(R.id.sms_sent_tv_content)
        tvContent.text = arguments?.getString(EXTRA_REPONSE)

    }

    override fun onResume() {
        super.onResume()
        tvContent.post({start()})
    }

    override fun onPause() {
        super.onPause()
        tvContent.handler?.removeCallbacksAndMessages(null)
    }

    private fun start() {
        if(isDetached){
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimUtils.reveal(card)
        } else {
            card.visibility = View.VISIBLE
        }

        tvContent.postDelayed({ end() }, 3000)
    }

    fun end() {
        if(isDetached){
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimUtils.unreveal(card, {dismiss()})
        } else {
            dismiss()
        }
    }

    private fun dismiss() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        activity?.moveTaskToBack(true)
        activity?.finish()
    }

}