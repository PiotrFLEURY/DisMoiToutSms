package fr.piotr.dismoitoutsms.intentannotations

import android.app.Activity
import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Created by piotr on 25/03/2018.
 *
 */

val intentAnnotationsHolder : MutableMap<String, KFunction<*>> = HashMap()
val intentAnnotationsReceivers : MutableMap<Any, IntentAnnocationReceiver> = HashMap()

const val TAG = "@IntentReceiver"

class IntentAnnocationReceiver(private val owner: Any): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: android.content.Intent?) {
        if(intentAnnotationsHolder.containsKey(intent?.action)) {
            val kFunction = intentAnnotationsHolder[intent?.action]!!
            val args = ArrayList<Any>()
            for (parameter in kFunction.parameters) {
                for (annotation in parameter.annotations) {
                    if (annotation is Extra) {
                        var extraName = annotation.value
                        if(extraName.isEmpty()){
                            extraName = parameter.name!!
                        }
                        args.add(intent?.extras!![extraName])
                    }
                }
            }
            kFunction.call(owner, *args.toArray())
        }
    }
}

val Fragment.intentAnnotationReceiver: IntentAnnocationReceiver
    get() = intentAnnotationsReceivers[this]!!

val Activity.intentAnnotationReceiver: IntentAnnocationReceiver
    get() = intentAnnotationsReceivers[this]!!

val android.support.v4.app.DialogFragment.intentAnnotationReceiver: IntentAnnocationReceiver
    get() = intentAnnotationsReceivers[this]!!

fun bindIntentAnnotations(any: Any) {
    Log.d(TAG, "${any::class} binding intents")
    bind(any)
    Log.d(TAG, "${any::class} binded")
}

private fun buildFilter(any: Any): IntentFilter {
    val filter = IntentFilter()
    Log.d(TAG, "${any::class} building intent filter")
    for (memberFunction in any::class.declaredFunctions) {
        val annotation = memberFunction.findAnnotation<IntentReceiver>()
        annotation?.let {
            parseIntent(memberFunction, annotation)
            filter.addAction(annotation.value)
        }
    }
    Log.d(TAG, "${any::class} filter built")
    return filter
}

fun unbindIntentAnnotations(any: Any){
    when(any){
        is Activity -> unbindActivityIntentAnnotations(any)
        is Fragment -> unbindFragmentIntentAnnotations(any)
        is android.support.v4.app.DialogFragment -> unbindFragmentIntentAnnotations(any)
    }
    intentAnnotationsReceivers.remove(any)
    val clazz = any::class
    for (memberFunction in clazz.memberFunctions) {
        for (annotation in memberFunction.annotations) {
            if (annotation is IntentReceiver) {
                intentAnnotationsHolder.remove(annotation.value)
            }
        }
    }
}

fun unbindFragmentIntentAnnotations(fragment: android.support.v4.app.DialogFragment) {
    if(intentAnnotationsReceivers.containsKey(fragment)){
        fragment.context?.let { LocalBroadcastManager.getInstance(it).unregisterReceiver(intentAnnotationsReceivers[fragment]!!) }
    }
}

fun unbindFragmentIntentAnnotations(fragment: Fragment) {
    if(intentAnnotationsReceivers.containsKey(fragment)){
        LocalBroadcastManager.getInstance(fragment.activity).unregisterReceiver(intentAnnotationsReceivers[fragment]!!)
    }
}

fun unbindActivityIntentAnnotations(activity: Activity) {
    if(intentAnnotationsReceivers.containsKey(activity)) {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(intentAnnotationsReceivers[activity]!!)
    }
}

fun bind(any: Any) {
    val filter = buildFilter(any)
    when(any){
        is Activity -> bindActivity(any, filter)
        is Fragment -> bindFragment(any, filter)
        is android.support.v4.app.DialogFragment -> bindFragment(any, filter)
        else -> throw IllegalArgumentException("Not managed element $any")
    }
}

fun bindFragment(fragment: android.support.v4.app.DialogFragment, filter: IntentFilter) {
    intentAnnotationsReceivers[fragment] = IntentAnnocationReceiver(fragment)
    fragment.context?.let { LocalBroadcastManager.getInstance(it).registerReceiver(fragment.intentAnnotationReceiver, filter) }
}

fun bindFragment(fragment: Fragment, filter: IntentFilter) {
    intentAnnotationsReceivers[fragment] = IntentAnnocationReceiver(fragment)
    LocalBroadcastManager.getInstance(fragment.activity).registerReceiver(fragment.intentAnnotationReceiver, filter)
}

fun bindActivity(activity: Activity, filter: IntentFilter) {
    intentAnnotationsReceivers[activity] = IntentAnnocationReceiver(activity)
    LocalBroadcastManager.getInstance(activity).registerReceiver(activity.intentAnnotationReceiver, filter)
}

fun parseIntent(memberFunction: KFunction<*>, annotation: IntentReceiver) {
    intentAnnotationsHolder[annotation.value] = memberFunction
}
