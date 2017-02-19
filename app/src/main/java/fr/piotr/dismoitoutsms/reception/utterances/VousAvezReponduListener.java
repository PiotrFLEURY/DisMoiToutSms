package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;

import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;

import static fr.piotr.dismoitoutsms.reception.TextToSpeechHelper.DITES_MODIFIER_ENVOYER_OU_FERMER;
import static fr.piotr.dismoitoutsms.util.Diction.MODIFIER_ENVOYER_OU_FERMER;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class VousAvezReponduListener extends UtteranceListener {

    public VousAvezReponduListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onDone(String utteranceId) {
        speech.parler(DITES_MODIFIER_ENVOYER_OU_FERMER, MODIFIER_ENVOYER_OU_FERMER);
    }
}
