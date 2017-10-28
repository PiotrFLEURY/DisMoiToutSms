package fr.piotr.dismoitoutsms.util;

import android.util.Log;

import fr.piotr.dismoitoutsms.SmsRecuActivity;

/**
 * @author Piotr
 * 
 */
public class Sablier extends Thread {

    private static final String TAG = "Sablier";

	public interface OnProgressListener{

		void onProgress(int value);
	}

	private static final int	TIMEOUT	= 60;

	private SmsRecuActivity	caller;
	private int				step;
	private boolean			finished;

	private OnProgressListener onProgressListener;

	public Sablier(SmsRecuActivity caller) {
		this.caller = caller;
		reset();
	}

	public void setOnProgressListener(OnProgressListener onProgressListener) {
		this.onProgressListener = onProgressListener;
	}

	@Override
	public void run() {
		try {
			reset();
			while (!finished && step < TIMEOUT) {
				Thread.sleep(1000);
				step++;
				if(onProgressListener!=null){
					onProgressListener.onProgress(TIMEOUT-step);
				}
			}
			if (!finished) {
				caller.finish();
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
