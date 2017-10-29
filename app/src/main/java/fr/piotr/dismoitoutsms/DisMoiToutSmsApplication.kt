package fr.piotr.dismoitoutsms

import android.app.ActivityManager
import android.app.Application
import android.app.Service
import android.content.Context
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService

/**
 * Created by piotr on 28/10/2017.
 *
 */
class DisMoiToutSmsApplication: Application() {

    companion object {

        lateinit var INSTANCE: DisMoiToutSmsApplication

    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    fun disMoiToutSmsServiceRunning(): Boolean {
        return isMyServiceRunning(DisMoiToutSmsService::class.java)
    }
    fun serviceCommunicatorRunning(): Boolean {
        return isMyServiceRunning(ServiceCommunicator::class.java)
    }

    private fun isMyServiceRunning(clazz: Class<out Service>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { clazz.name == it.service.className }
    }

}