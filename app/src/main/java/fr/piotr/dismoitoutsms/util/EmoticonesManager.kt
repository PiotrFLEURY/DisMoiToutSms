package fr.piotr.dismoitoutsms.util

import android.content.Context

fun remplacerEmoticones(context: Context, phrase: String): String {

    var toReturn = phrase
    for (emo in Emoticone.values()) {

        while (toReturn.contains(emo.code)) {

            toReturn = toReturn
                    .replace(emo.code, context.getString(emo.remplacement))

        }
    }

    return toReturn
}