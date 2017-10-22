package fr.piotr.dismoitoutsms.reception;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

public class ServiceCommunicator extends Service {

	private SmsReceiver		mSMSreceiver;
	private StepListener 	stepListener;

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

        boolean stepDetectorActivated = ConfigurationManager.getBoolean(getApplicationContext(), ConfigurationManager.Configuration.ARRET_STEP_DETECTOR);
        if(stepDetectorActivated && isKitKatWithStepCounter()){
            registerListeners();
        }


	}

    void registerListeners() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			stepListener = new StepListener(this);
			SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
			sensorManager.registerListener(stepListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
    }

    void unregisterListeners() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			sensorManager.unregisterListener(stepListener);
		}
    }

	@Override
	public void onDestroy() {
		Log.i("DisMoiToutSms", "ServiceCommunicator destroy !");
		super.onDestroy();

		// Unregister the SMS receiver
		unregisterReceiver(mSMSreceiver);

		deleteNotification();

        unregisterListeners();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void createNotification() {
		NotificationHelper.open(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID);
	}

	private void deleteNotification() {
		NotificationHelper.close(this, NotificationHelper.SERVICE_STARTED_COMPLEX_ID);
	}

	public boolean isKitKatWithStepCounter() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			PackageManager pm = getPackageManager();
			return pm.hasSystemFeature (PackageManager.FEATURE_SENSOR_STEP_DETECTOR)
                    && pm.hasSystemFeature (PackageManager.FEATURE_SENSOR_STEP_COUNTER);
		}
        return false;
	}

}