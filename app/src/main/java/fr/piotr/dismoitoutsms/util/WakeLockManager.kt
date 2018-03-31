package fr.piotr.dismoitoutsms.util

import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.view.WindowManager

fun FragmentActivity.setWakeUp() {

    window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

}

fun DialogFragment.setWakeUp() {
    dialog.window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}