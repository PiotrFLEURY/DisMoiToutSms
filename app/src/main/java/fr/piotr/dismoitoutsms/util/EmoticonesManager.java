package fr.piotr.dismoitoutsms.util;

import android.content.Context;

public class EmoticonesManager {

	private static EmoticonesManager	instance;

	public static EmoticonesManager getInstance() {
		if (instance == null) {
			instance = new EmoticonesManager();
		}
		return instance;
	}

	public String remplacer(Context context, String phrase) {

		String toReturn = phrase;
		for (Emoticone emo : Emoticone.values()) {

			while (toReturn.contains(emo.getCode())) {

				toReturn = toReturn
						.replace(emo.getCode(), context.getString(emo.getRemplacement()));

			}
		}

		return toReturn;
	}

}
