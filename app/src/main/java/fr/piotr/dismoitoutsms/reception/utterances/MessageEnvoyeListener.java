package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;

import fr.piotr.dismoitoutsms.reception.SmsReceiver;
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class MessageEnvoyeListener extends UtteranceListener {

    public MessageEnvoyeListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onDone(String utteranceId) {
        SmsReceiver.getInstance().nextMessage(context);
    }
}
