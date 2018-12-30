package fr.piotr.dismoitoutsms

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Telephony
import android.widget.Toast
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.appcompat.app.AppCompatActivity
import fr.piotr.dismoitoutsms.util.TransitionEndListener
import kotlinx.android.synthetic.main.splash_screen.*

class SpashActivity : AppCompatActivity() {

    private val handler = Handler()
    private val setStart = androidx.constraintlayout.widget.ConstraintSet()
    private val setFinished = androidx.constraintlayout.widget.ConstraintSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinished.clone(this, R.layout.splash_screen)

        setContentView(R.layout.splash_screen_start)
        setStart.clone(spash_constraint)

        handler.postDelayed({animateLayout()}, 500)

    }

    override fun onResume() {
        super.onResume()
        ic_message.setOnClickListener{openThreads()}
        ic_settings.setOnClickListener{openMain()}
    }

    override fun onPause() {
        super.onPause()
        ic_message.setOnClickListener{}
        ic_settings.setOnClickListener{}
    }

    private fun animateLayout() {
        val transition = AutoTransition()
        transition.duration = 400
//        transition.addListener(TransitionEndListener {openMain()})
        TransitionManager.beginDelayedTransition(spash_constraint, transition)
        setFinished.applyTo(spash_constraint)

        declareAsDefaultSmsHandler()
    }

    private fun openMain() {
        startActivity(Intent(this, DisMoiToutSmsActivity::class.java))
    }

    private fun openThreads() {
        startActivity(Intent(this, ThreadsActivity::class.java))
    }

    private fun declareAsDefaultSmsHandler() {
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }

}