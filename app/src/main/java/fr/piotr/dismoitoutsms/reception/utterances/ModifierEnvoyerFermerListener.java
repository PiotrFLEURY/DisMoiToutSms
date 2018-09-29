package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import fr.piotr.dismoitoutsms.SmsRecuActivity;
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;

import static fr.piotr.dismoitoutsms.reception.TextToSpeechHelper.DITES_MODIFIER_ENVOYER_OU_FERMER;
import static fr.piotr.dismoitoutsms.util.Instruction.MODIFIER_ENVOYER_FERMER;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class ModifierEnvoyerFermerListener extends UtteranceListener {

    public ModifierEnvoyerFermerListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onDone(String utteranceId) {
        Intent intent = new Intent(SmsRecuActivity.EVENT_START_SPEECH_RECOGNIZER);
        intent.putExtra(SmsRecuActivity.EXTRA_INSTRUCTION, MODIFIER_ENVOYER_FERMER);
        intent.putExtra(SmsRecuActivity.EXTRA_PROMPT, DITES_MODIFIER_ENVOYER_OU_FERMER);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//        SmsRecuActivity.startVoiceRecognition(MODIFIER_ENVOYER_FERMER, DITES_MODIFIER_ENVOYER_OU_FERMER);
    }

}
