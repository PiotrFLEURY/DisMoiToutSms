package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;
import android.telephony.TelephonyManager;

import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;

import static fr.piotr.dismoitoutsms.reception.TextToSpeechHelper.DITES_LIRE_OU_FERMER;
import static fr.piotr.dismoitoutsms.util.Diction.LIRE_OU_FERMER;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class MessageRecuModeViePriveeUtteranceListener extends UtteranceListener {

    public MessageRecuModeViePriveeUtteranceListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onDone(String utteranceId) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager.getCallState()==TelephonyManager.CALL_STATE_IDLE) {
            speech.parler(DITES_LIRE_OU_FERMER, LIRE_OU_FERMER);
        }
    }

}
