package fr.piotr.dismoitoutsms.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import fr.piotr.dismoitoutsms.R;

public class MessageBox {

	public static void confirm(Context context, String title, String message, final Runnable ok,
			final Runnable ko) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(context.getString(R.string.oui),
				(dialog, which) -> {
                    if (ok != null) {
                        ok.run();
                    }
                });
		builder.setNegativeButton(context.getString(R.string.non),
				(dialog, which) -> {
                    if (ko != null) {
                        ko.run();
                    }
                });
		AlertDialog dialog = builder.create();
		dialog.show();
	}

}
