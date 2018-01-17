package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;
import android.telephony.TelephonyManager;

import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

import static fr.piotr.dismoitoutsms.reception.TextToSpeechHelper.DITES_REPONDRE_OU_FERMER;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean;
import static fr.piotr.dismoitoutsms.util.Diction.REPONDRE_OU_FERMER;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class MessageRecuUtteranceListener extends UtteranceListener {

    public MessageRecuUtteranceListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onUtteranceCompleted(String utteranceId) {
        boolean reponseActivee = getBoolean(context, ConfigurationManager.Configuration.COMMANDE_VOCALE);
        if (reponseActivee) {
            super.onUtteranceCompleted(utteranceId);
        }
    }

    @Override
    public void onDone(String utteranceId) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager.getCallState()==TelephonyManager.CALL_STATE_IDLE) {
            speech.parler(DITES_REPONDRE_OU_FERMER, REPONDRE_OU_FERMER);
        }
    }

}
