package fr.piotr.dismoitoutsms.bootreceiver

import android.app.Service
import android.content.*
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService

/**
 * Created by piotr on 09/07/2017.
 *
 */

class BootReceiver : BroadcastReceiver() {

    private var mBound: Boolean = false

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            mBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBound = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && canStartService(context)) {
            context.bindService(Intent(context, DisMoiToutSmsService::class.java), mServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun canStartService(context: Context) : Boolean {
        if (mBound) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }
}
