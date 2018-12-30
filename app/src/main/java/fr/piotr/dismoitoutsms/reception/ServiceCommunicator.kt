package fr.piotr.dismoitoutsms.reception

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log

import fr.piotr.dismoitoutsms.util.NotificationHelper

class ServiceCommunicator : Service() {

    private var mSMSreceiver: SmsReceiver? = null
    private val mBinder = ServiceCommunicatorBinder()

    inner class ServiceCommunicatorBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: ServiceCommunicator
            get() = this@ServiceCommunicator
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return Service.START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        // SMS event receiver
        mSMSreceiver = SmsReceiver()
        val mIntentFilter = IntentFilter()
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(mSMSreceiver, mIntentFilter)

        Log.i("DisMoiToutSms", "Service d'écoute des SMS démarré")

        createNotification()

    }

    override fun onDestroy() {
        Log.i("DisMoiToutSms", "ServiceCommunicator destroy !")
        super.onDestroy()

        // Unregister the SMS receiver
        unregisterReceiver(mSMSreceiver)

        deleteNotification()

    }

    private fun createNotification() {
        NotificationHelper.open(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID)
    }

    private fun deleteNotification() {
        NotificationHelper.close(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID)
    }

}