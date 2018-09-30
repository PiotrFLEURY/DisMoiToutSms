package fr.piotr.dismoitoutsms.service

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log

import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import fr.piotr.dismoitoutsms.DisMoiToutSmsActivity
import fr.piotr.dismoitoutsms.headset.BluetoothReceiver
import fr.piotr.dismoitoutsms.headset.HeadSetReceiver
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator
import fr.piotr.dismoitoutsms.util.NotificationHelper
import java.lang.Exception

/**
 * Created by piotr on 09/07/2017.
 *
 */

class DisMoiToutSmsService : Service() {

    private var headSetReceiver: HeadSetReceiver? = null
    private var bluetoothReceiver: BluetoothReceiver? = null
    private val mBinder = DisMoiToutSmsServiceBinder()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                INTENT_ACTIVATE_FROM_NOTIFICATION -> activateFromNotification()
                INTENT_DEACTIVATE_FROM_NOTIFICATION -> deactivateFromNotification()
            }
        }
    }

    inner class DisMoiToutSmsServiceBinder : Binder() {
        val service: DisMoiToutSmsService
            get() = this@DisMoiToutSmsService
    }

    var serviceCommunicatorBound = false

    private val mServiceCommunicatorServiceConnection = object : ServiceConnection {

        var mService: ServiceCommunicator? = null

        override fun onServiceDisconnected(p0: ComponentName?) {
            serviceCommunicatorBound = false
            Log.d(DisMoiToutSmsService::class.java.simpleName, "service communicator disconnected")
        }

        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            serviceCommunicatorBound = true
            mService = (binder as ServiceCommunicator.ServiceCommunicatorBinder).service
            LocalBroadcastManager.getInstance(binder.service).sendBroadcast(Intent(DisMoiToutSmsActivity.EVENT_TOGGLE_STATUS))
            Log.d(DisMoiToutSmsService::class.java.simpleName, "service communicator connected")
        }

    }

    private fun activateFromNotification() {
        startServiceCommunicator()
        NotificationHelper.close(this, NotificationHelper.HEADSET_PLUGGED_IN)
    }

    fun startServiceCommunicator() {
        if(!serviceCommunicatorBound) {
            Log.d(DisMoiToutSmsService::class.java.simpleName, "starting service communicator...")
            applicationContext.bindService(Intent(this, ServiceCommunicator::class.java),
                    mServiceCommunicatorServiceConnection, BIND_AUTO_CREATE)
        }
    }

    fun stopServiceCommunicator() {
        if(serviceCommunicatorBound) {
            Log.d(DisMoiToutSmsService::class.java.simpleName, "stopping service communicator...")
            applicationContext.unbindService(mServiceCommunicatorServiceConnection)
            serviceCommunicatorBound = false
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(DisMoiToutSmsActivity.EVENT_TOGGLE_STATUS))
        }
    }

    private fun deactivateFromNotification() {
        stopServiceCommunicator()
        NotificationHelper.close(this, NotificationHelper.HEADSET_PLUGGED_IN)
        //NotificationHelper.close(this, NotificationHelper.SERVICE_STARTED_ID);
        NotificationHelper.close(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID)
        NotificationHelper.close(this, NotificationHelper.STOPPED_BY_STEP_COUNTER)
    }

    override fun onCreate() {
        super.onCreate()
        headSetReceiver = HeadSetReceiver(this)
        bluetoothReceiver = BluetoothReceiver(this)

        startService(Intent(this, DisMoiToutSmsService::class.java))

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val filter = IntentFilter()
        filter.addAction(INTENT_ACTIVATE_FROM_NOTIFICATION)
        filter.addAction(INTENT_DEACTIVATE_FROM_NOTIFICATION)
        applicationContext.registerReceiver(receiver, filter)
        applicationContext.registerReceiver(headSetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        val bluetoothIntentFilter = IntentFilter()
        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        //bluetoothIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        applicationContext.registerReceiver(bluetoothReceiver, bluetoothIntentFilter)

        Log.d(DisMoiToutSmsService::class.java.simpleName, "Service started")

        return Service.START_STICKY
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        try {
            applicationContext.unregisterReceiver(receiver)
            applicationContext.unregisterReceiver(headSetReceiver)
            applicationContext.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(DisMoiToutSmsService::class.java.simpleName, e.message)
        }
        super.onDestroy()
        Log.d(DisMoiToutSmsService::class.java.simpleName, "Service destroyed")
    }

    companion object {

        val TAG = "DisMoiToutSmsService"

        val INTENT_ACTIVATE_FROM_NOTIFICATION = "$TAG.INTENT_ACTIVATE_FROM_NOTIFICATION"
        val INTENT_DEACTIVATE_FROM_NOTIFICATION = "$TAG.INTENT_DEACTIVATE_FROM_NOTIFICATION"
    }
}
