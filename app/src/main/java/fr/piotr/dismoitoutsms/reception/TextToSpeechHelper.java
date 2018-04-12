package fr.piotr.dismoitoutsms.reception;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import fr.piotr.dismoitoutsms.BuildConfig;
import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.reception.utterances.ContactsTrouvesListener;
import fr.piotr.dismoitoutsms.reception.utterances.LireOuFermerListener;
import fr.piotr.dismoitoutsms.reception.utterances.MessageEnvoyeListener;
import fr.piotr.dismoitoutsms.reception.utterances.MessageRecuModeViePriveeUtteranceListener;
import fr.piotr.dismoitoutsms.reception.utterances.MessageRecuUtteranceListener;
import fr.piotr.dismoitoutsms.reception.utterances.ModifierEnvoyerFermerListener;
import fr.piotr.dismoitoutsms.reception.utterances.RepondreOuFermerListener;
import fr.piotr.dismoitoutsms.reception.utterances.UtteranceListener;
import fr.piotr.dismoitoutsms.reception.utterances.VousAvezReponduListener;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.Diction;

import static fr.piotr.dismoitoutsms.util.EmoticonesManagerKt.remplacerEmoticones;

/**
 * Created by piotr_000 on 19/03/2016.
 *
 */
public class TextToSpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "TextToSpeechHelper";

    public interface StartedListener {
        void onStarted();
    }

    public static String DITES_LIRE_OU_FERMER;
    public static String DITES_REPONDRE_OU_FERMER;
    public static String DITES_MODIFIER_ENVOYER_OU_FERMER;

    private Context context;

    private TextToSpeech	mTts;
    private Map<Diction, UtteranceListener> listeners = new HashMap<>();

    private StartedListener listener;

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private AudioFocusRequest audioFocusRequest;

    public TextToSpeechHelper(Context context, StartedListener listener) {
        this.context=context;
        this.listener=listener;
        this.mTts = new TextToSpeech(context, this);

        DITES_LIRE_OU_FERMER = getString(R.string.dites) + " " + getString(R.string.listen)
                + " " + getString(R.string.ou) + " " + getString(R.string.fermer);
        DITES_REPONDRE_OU_FERMER = getString(R.string.dites) + " " + getString(R.string.repeat)
                + " " + getString(R.string.repondre)
                + " " + getString(R.string.ou) + " " + getString(R.string.fermer);
        DITES_MODIFIER_ENVOYER_OU_FERMER = getString(R.string.dites) + " " + getString(R.string.ajouter) + ", " + getString(R.string.modifier)
                + ", " + getString(R.string.envoyer) + " " + getString(R.string.ou) + " "
                + getString(R.string.fermer);

        listeners.put(Diction.MESSAGE_RECU_MODE_VIE_PRIVEE, new MessageRecuModeViePriveeUtteranceListener(context, this));
        listeners.put(Diction.MESSAGE_RECU, new MessageRecuUtteranceListener(context, this));
        listeners.put(Diction.LIRE_OU_FERMER, new LireOuFermerListener(context, this));
        listeners.put(Diction.REPETER_REPONDRE_OU_FERMER, new RepondreOuFermerListener(context, this));
        listeners.put(Diction.VOUS_AVEZ_REPONDU, new VousAvezReponduListener(context, this));
        listeners.put(Diction.MODIFIER_ENVOYER_OU_FERMER, new ModifierEnvoyerFermerListener(context, this));
        listeners.put(Diction.MESSAGE_ENVOYE, new MessageEnvoyeListener(context, this));
        listeners.put(Diction.CONTACTS_TROUVES, new ContactsTrouvesListener(context, this));

        this.audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int i) {
                if(BuildConfig.DEBUG){
                    switch (i){
                        case AudioManager.AUDIOFOCUS_GAIN:
                            Log.d(getClass().getSimpleName(), "AUDIOFOCUS_GAIN");
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            Log.d(getClass().getSimpleName(), "AUDIOFOCUS_LOSS");
                            break;
                    }
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes mPlaybackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            this.audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(mPlaybackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(this.audioFocusChangeListener, new Handler())
                    .build();
        }
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

        requestAudioFocus();

        if (ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.EMOTICONES)) {
            text = remplacerEmoticones(context, text);
        }

        registerUtteranceListener(type);

        speak(text, type);
    }

    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioManager.requestAudioFocus(audioFocusRequest);
        }
    }

    @SuppressWarnings("deprecation")
    private void speak(String text, Diction type) {
        String utteranceId = type.name();
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP) {
            HashMap<String, String> parametres = new HashMap<>();
            parametres.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            parametres.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, parametres);
        } else {
            final Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            params.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        }
    }

    @SuppressWarnings("deprecation")
    private void registerUtteranceListener(Diction type) {
        final UtteranceListener utteranceListener = listeners.get(type);
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                //Nothing to do here
            }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceListener != null) {
                    utteranceListener.onUtteranceCompleted(utteranceId);
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(getClass().getName(), String.format("ERROR utteranceId=%s", utteranceId));
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                super.onError(utteranceId, errorCode);
                Log.e(getClass().getName(), String.format("ERROR utteranceId=%s errorCode=%s", utteranceId, errorCode));
            }
        });
    }

    public void stopLecture() {
        abandonAudioFocus();
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

    public void abandonAudioFocus() {
        if(BuildConfig.DEBUG){
            Log.d(getClass().getSimpleName(), "abandonAudioFocus()");
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O ) {
            Log.d(getClass().getSimpleName(), "abandonAudioFocus().audioFocusChangeListener");
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            Log.d(getClass().getSimpleName(), "abandonAudioFocus().audioFocusRequest");
        }
    }
}
