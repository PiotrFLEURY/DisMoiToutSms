package fr.piotr.dismoitoutsms

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import fr.piotr.dismoitoutsms.util.TransitionEndListener
import kotlinx.android.synthetic.main.splash_screen.*

class SpashActivity : AppCompatActivity() {

    private val handler = Handler()
    private val setStart = ConstraintSet()
    private val setFinished = ConstraintSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinished.clone(this, R.layout.splash_screen)

        setContentView(R.layout.splash_screen_start)
        setStart.clone(spash_constraint)

        handler.postDelayed({animateLayout()}, 500)

    }

    fun animateLayout() {
        val transition = AutoTransition()
        transition.duration = 400
        transition.addListener(TransitionEndListener({openMain()}))
        TransitionManager.beginDelayedTransition(spash_constraint, transition)
        setFinished.applyTo(spash_constraint)
    }

    private fun openMain() {
        startActivity(Intent(this, DisMoiToutSmsActivity::class.java))
        finish()
    }


}