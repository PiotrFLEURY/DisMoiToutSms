package fr.piotr.dismoitoutsms.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService

/**
 * Created by piotr on 09/07/2017.
 *
 */

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startService(Intent(context, DisMoiToutSmsService::class.java))
        }
    }
}
