package fr.piotr.dismoitoutsms.reception;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import fr.piotr.dismoitoutsms.util.NotificationHelper;

public class ServiceCommunicator extends Service {

	private SmsReceiver		mSMSreceiver;
	private final IBinder mBinder = new ServiceCommunicatorBinder();

	public class ServiceCommunicatorBinder extends Binder {
		public ServiceCommunicator getService() {
			// Return this instance of LocalService so clients can call public methods
			return ServiceCommunicator.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// SMS event receiver
		mSMSreceiver = new SmsReceiver();
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
		registerReceiver(mSMSreceiver, mIntentFilter);

		Log.i("DisMoiToutSms", "Service d'écoute des SMS démarré");

		createNotification();

	}

	@Override
	public void onDestroy() {
		Log.i("DisMoiToutSms", "ServiceCommunicator destroy !");
		super.onDestroy();

		// Unregister the SMS receiver
		unregisterReceiver(mSMSreceiver);

		deleteNotification();

	}

	private void createNotification() {
		NotificationHelper.open(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID);
	}

	private void deleteNotification() {
		NotificationHelper.close(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID);
	}

}