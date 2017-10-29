package fr.piotr.dismoitoutsms.util;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication;
import fr.piotr.dismoitoutsms.SmsRecuActivity;

/**
 * @author Piotr
 * 
 */
public class Sablier extends Thread {

    private static final String TAG = "Sablier";

	private static final int	TIMEOUT	= 60;

	private int				step;
	private boolean			finished;

	public Sablier() {
		reset();
	}

	@Override
	public void run() {
		try {
			reset();
			while (!finished && step < TIMEOUT) {
				Thread.sleep(1000);
				step++;
			}
			if (!finished) {
				Intent intent = new Intent(SmsRecuActivity.Companion.getEVENT_FINISH());
				LocalBroadcastManager.getInstance(DisMoiToutSmsApplication.INSTANCE.getApplicationContext()).sendBroadcast(intent);
			}
		} catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
		}
	}

	public void reset() {
		step = 0;
		finished = false;
	}

	public void finished() {
		finished = true;
	}

}
