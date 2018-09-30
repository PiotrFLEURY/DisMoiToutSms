package fr.piotr.dismoitoutsms

import android.app.Application

/**
 * Created by piotr on 28/10/2017.
 *
 */
class DisMoiToutSmsApplication: Application() {

    companion object {

        lateinit var INSTANCE: DisMoiToutSmsApplication//FIXME use managers with injectors

    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

}