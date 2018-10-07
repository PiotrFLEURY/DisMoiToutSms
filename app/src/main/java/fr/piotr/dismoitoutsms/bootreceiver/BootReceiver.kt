package fr.piotr.dismoitoutsms.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService

/**
 * Created by piotr on 09/07/2017.
 *
 */

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "intent received")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && canStartService(context)) {
            Log.d(TAG, "starting DisMoiToutSmsService...")
            context.startService(Intent(context, DisMoiToutSmsService::class.java))
        }
    }

    private fun canStartService(context: Context) : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }
}
