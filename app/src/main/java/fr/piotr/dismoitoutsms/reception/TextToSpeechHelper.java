package fr.piotr.dismoitoutsms.reception;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.reception.utterances.MessageEnvoyeListener;
import fr.piotr.dismoitoutsms.reception.utterances.MessageRecuUtteranceListener;
import fr.piotr.dismoitoutsms.reception.utterances.ModifierEnvoyerFermerListener;
import fr.piotr.dismoitoutsms.reception.utterances.RepondreOuFermerListener;
import fr.piotr.dismoitoutsms.reception.utterances.UtteranceListener;
import fr.piotr.dismoitoutsms.reception.utterances.VousAvezReponduListener;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.Diction;
import fr.piotr.dismoitoutsms.util.EmoticonesManager;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class TextToSpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "TextToSpeechHelper";

    public interface StartedListener {
        void onStarted();
    }

    public static String DITES_REPONDRE_OU_FERMER;
    public static String DITES_MODIFIER_ENVOYER_OU_FERMER;

    private Context context;

    private final TextToSpeech	mTts;
    private Map<Diction, UtteranceListener> listeners = new HashMap<>();

    private StartedListener listener;


    public TextToSpeechHelper(Context context, StartedListener listener) {
        this.context=context;
        this.listener=listener;
        this.mTts = new TextToSpeech(context, this);

        DITES_REPONDRE_OU_FERMER = getString(R.string.dites) + " " + getString(R.string.repondre)
                + " " + getString(R.string.ou) + " " + getString(R.string.fermer);
        DITES_MODIFIER_ENVOYER_OU_FERMER = getString(R.string.dites) + " " + getString(R.string.modifier)
                + ", " + getString(R.string.envoyer) + " " + getString(R.string.ou) + " "
                + getString(R.string.fermer);

        listeners.put(Diction.MESSAGE_RECU, new MessageRecuUtteranceListener(context, this));
        listeners.put(Diction.REPONDRE_OU_FERMER, new RepondreOuFermerListener(context, this));
        listeners.put(Diction.VOUS_AVEZ_REPONDU, new VousAvezReponduListener(context, this));
        listeners.put(Diction.MODIFIER_ENVOYER_OU_FERMER, new ModifierEnvoyerFermerListener(context, this));
        listeners.put(Diction.MESSAGE_ENVOYE, new MessageEnvoyeListener(context, this));
    }

    public void onInit(int status) {
        // vérification de la disponibilité de la synthèse vocale.
        if (status == TextToSpeech.SUCCESS) {
            // le choix de la langue ici français
            int result = mTts.setLanguage(ConfigurationManager.getLangueSelectionnee(context));
            // vérification ici si cette langue est supporté par le terminal et
            // si elle existe
//            mTts.setOnUtteranceCompletedListener(this);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // renvoi une erreur sur la console logcat.
                Log.e("DisMoiToutSms", "Language is not available.");
            }
            listener.onStarted();
        } else {
            // si la synthèse vocal n'est pas disponible
            Log.e("DisMoiToutSms", "Could not initialize TextToSpeech.");

        }
    }

    public void parler(String text, Diction type) {
        if (ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.EMOTICONES)) {
            text = EmoticonesManager.getInstance().remplacer(context, text);
        }

        registerUtteranceListener(type);

        speak(text, type);
    }

    @SuppressWarnings("deprecation")
    private void speak(String text, Diction type) {
        String utteranceId = type.name();
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP) {
            HashMap<String, String> parametres = new HashMap<>();
            parametres.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, parametres);
        } else {
            final Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        }
    }

    @SuppressWarnings("deprecation")
    private void registerUtteranceListener(Diction type) {
        final UtteranceListener utteranceListener = listeners.get(type);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTts.setOnUtteranceCompletedListener(utteranceListener);
        } else {
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    //Nothing to do here
                }

                @Override
                public void onDone(String utteranceId) {
                    if (utteranceListener != null) {
                        utteranceListener.onDone(utteranceId);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(getClass().getName(), String.format("ERROR utteranceId=%s", utteranceId));
                }
            });
        }
    }

    public void stopLecture() {
        mTts.stop();
    }

    public void shutdown() {
        try {
            mTts.shutdown();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public String getString(int resId) {
        return context.getString(resId);
    }
}
