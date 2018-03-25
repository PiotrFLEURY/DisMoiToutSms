package fr.piotr.dismoitoutsms.intentannotations

/**
 * Created by piotr on 25/03/2018.
 *
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Extra(val value:String = "")