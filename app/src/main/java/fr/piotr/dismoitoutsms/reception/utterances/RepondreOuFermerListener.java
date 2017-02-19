package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import fr.piotr.dismoitoutsms.SmsRecuActivity;
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;

import static fr.piotr.dismoitoutsms.reception.TextToSpeechHelper.DITES_REPONDRE_OU_FERMER;
import static fr.piotr.dismoitoutsms.util.Instruction.REPONDRE_FERMER;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class RepondreOuFermerListener extends UtteranceListener {

    public RepondreOuFermerListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onDone(String utteranceId) {
        Intent intent = new Intent(SmsRecuActivity.EVENT_START_SPEECH_RECOGNIZER);
        intent.putExtra(SmsRecuActivity.EXTRA_INSTRUCTION, REPONDRE_FERMER);
        intent.putExtra(SmsRecuActivity.EXTRA_PROMPT, DITES_REPONDRE_OU_FERMER);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//        SmsRecuActivity.startVoiceRecognition(REPONDRE_FERMER, DITES_REPONDRE_OU_FERMER);
    }

}
