package fr.piotr.dismoitoutsms.ktx

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager

val Activity.localIntentFilter : IntentFilter
    get() {
        return IntentFilter()
    }

fun Activity.bind(action:String, method: ()->Unit) {
    binding[action] = method
    localIntentFilter.addAction(action)
}

val Activity.binding : MutableMap<String, ()->Unit>
    get() {
        return mutableMapOf()
    }

val Activity.localBroadCastReceiver
        get () = object : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        binding[intent?.action]?.invoke()
    }

}

fun Activity.registerLocalReceiver() {
    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(localBroadCastReceiver, localIntentFilter)
}

fun Activity.unregisterLocalReceiver() {
    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadCastReceiver)
}

fun IntentFilter.addActions(vararg actions:String) : IntentFilter {
    for(action in actions){
        addAction(action)
    }
    return this
}