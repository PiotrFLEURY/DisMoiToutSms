package fr.piotr.dismoitoutsms.util

import android.support.transition.Transition

class TransitionEndListener(private val todo:()->Unit): Transition.TransitionListener {

    override fun onTransitionEnd(transition: Transition) {
        todo.invoke()
    }

    override fun onTransitionResume(transition: Transition) {
        //
    }

    override fun onTransitionPause(transition: Transition) {
        //
    }

    override fun onTransitionCancel(transition: Transition) {
        //
    }

    override fun onTransitionStart(transition: Transition) {
        //
    }
}