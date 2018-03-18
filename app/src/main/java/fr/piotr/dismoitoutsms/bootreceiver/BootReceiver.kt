package fr.piotr.dismoitoutsms.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService
import fr.piotr.dismoitoutsms.util.AbstractActivity

/**
 * Created by piotr on 09/07/2017.
 *
 */

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && canStartService(context)) {
            context.startService(Intent(context, DisMoiToutSmsService::class.java))
        }
    }

    private fun canStartService(context: Context) : Boolean {
        if (DisMoiToutSmsApplication.INSTANCE.disMoiToutSmsServiceRunning()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(AbstractActivity.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }
}
