package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.SmsRecuActivity;
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;

import static fr.piotr.dismoitoutsms.util.Instruction.DICTER_CONTACT;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class ContactsTrouvesListener extends UtteranceListener {

    public ContactsTrouvesListener(Context context, TextToSpeechHelper speech) {
        super(context, speech);
    }

    @Override
    public void onDone(String utteranceId) {
        Intent intent = new Intent(SmsRecuActivity.EVENT_START_SPEECH_RECOGNIZER);
        intent.putExtra(SmsRecuActivity.EXTRA_INSTRUCTION, DICTER_CONTACT);
        intent.putExtra(SmsRecuActivity.EXTRA_PROMPT, context.getString(R.string.dictate_contact_name));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
