package fr.piotr.dismoitoutsms.reception.utterances;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;

import java.util.List;

import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

import static fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public abstract class UtteranceListener implements TextToSpeech.OnUtteranceCompletedListener {

    TextToSpeechHelper speech;
    Context context;

    UtteranceListener(Context context, TextToSpeechHelper speech){
        this.context = context;
        this.speech = speech;
    }

    public final void onUtteranceCompleted(String utteranceId) {
        boolean reponseActivee = getBoolean(context, ConfigurationManager.Configuration.COMMANDE_VOCALE);
        if (reponseActivee && isReconnaissanceVocaleInstallee()) {
            onDone(utteranceId);
        }
    }

    public void onDone(String utteranceId){
        speech.abandonAudioFocus();
    }

    private boolean isReconnaissanceVocaleInstallee() {
        PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        return activities.size() > 0;
    }
}
