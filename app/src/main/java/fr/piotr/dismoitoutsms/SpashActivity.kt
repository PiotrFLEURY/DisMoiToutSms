package fr.piotr.dismoitoutsms

import android.content.Intent
import android.os.Bundle
import android.os.Handler
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

    private fun animateLayout() {
        val transition = AutoTransition()
        transition.duration = 400
        transition.addListener(TransitionEndListener {openMain()})
        TransitionManager.beginDelayedTransition(spash_constraint, transition)
        setFinished.applyTo(spash_constraint)
    }

    private fun openMain() {
        startActivity(Intent(this, DisMoiToutSmsActivity::class.java))
        finish()
    }


}