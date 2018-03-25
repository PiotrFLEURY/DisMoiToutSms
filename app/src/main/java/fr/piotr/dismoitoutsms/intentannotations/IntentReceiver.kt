package fr.piotr.dismoitoutsms.intentannotations

/**
 * Created by piotr on 25/03/2018.
 *
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntentReceiver(val value:String)