package fr.piotr.dismoitoutsms.speech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.SmsRecuActivity;
import fr.piotr.dismoitoutsms.util.Instruction;

import static android.speech.RecognizerIntent.EXTRA_CALLING_PACKAGE;
import static android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL;
import static android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS;
import static android.speech.RecognizerIntent.EXTRA_PROMPT;
import static android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;

/**
 * Created by piotr_000 on 13/03/2016.
 *
 */
public class MySpeechRecorder implements RecognitionListener {

    private static final String TAG = "MySpeechRecorder";

//    public static final int MIN_TIME = 30000;

    private SmsRecuActivity context;
    private Instruction instruction;
    private String extraPrompt;

    private SpeechRecognizer speech;

    public MySpeechRecorder(SmsRecuActivity context){
        this.context=context;
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        speech.setRecognitionListener(this);
    }

    public void startListening(Instruction instruction, String extraPrompt){
        this.instruction = instruction;
        this.extraPrompt = extraPrompt;
        startListening();
    }

    private void startListening() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        intent.putExtra(EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(EXTRA_PROMPT, extraPrompt);
        intent.putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, MIN_TIME);
//        intent.putExtra(EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MIN_TIME);
//        intent.putExtra(EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, MIN_TIME);
        speech.startListening(intent);
    }

    public void onReadyForSpeech(Bundle params) {

    }

    public void onBeginningOfSpeech() {

    }

    public void onRmsChanged(float rmsdB) {

    }

    public void onBufferReceived(byte[] buffer) {

    }

    public void onEndOfSpeech() {
        context.hideMicrophone();
    }

    public void onError(int error) {
        Log.d(TAG, "Error was thrown by SpeechRecognizer "+error);
        end(Collections.<String>emptyList(), Activity.RESULT_CANCELED);
    }

    public void onResults(Bundle results) {
        Log.d(TAG, "onResults");
        end(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION), Activity.RESULT_OK);
    }

    private void end(List<String> words, int returnCode) {
        stop();
        context.hideMicrophone();
        context.onSpeechResult(instruction, returnCode, words);
    }

    public void stop() {
        context.runOnUiThread(() -> {
            try {
                speech.stopListening();
                speech.destroy();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            }
        });

    }

    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> results = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(results==null) {
            return;
        }
        context.onPartialResult(results);
        String firstResult = results.get(0);
        switch (instruction) {
            case REPONDRE_FERMER:
                if(firstResult.equalsIgnoreCase(context.getString(R.string.repondre))
                        || firstResult.equalsIgnoreCase(context.getString(R.string.fermer))){
                    Log.d(TAG, "Premature end of recognization on partial result");
                    onResults(partialResults);
                }
                break;
            case MODIFIER_ENVOYER_FERMER:
                if(firstResult.equalsIgnoreCase(context.getString(R.string.modifier))
                        || firstResult.equalsIgnoreCase(context.getString(R.string.envoyer))
                        || firstResult.equalsIgnoreCase(context.getString(R.string.fermer))){
                    Log.d(TAG, "Premature end of recognization on partial result");
                    onResults(partialResults);
                }
                break;
            default:break;
        }
    }

    public void onEvent(int eventType, Bundle params) {

    }
}
