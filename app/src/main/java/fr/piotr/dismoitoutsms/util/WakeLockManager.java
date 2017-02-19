package fr.piotr.dismoitoutsms.util;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

public class WakeLockManager {
	public static void setWakeUp(Context context) {
		Window wnd = ((Activity) context).getWindow();
		wnd.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}