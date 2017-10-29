package fr.piotr.dismoitoutsms.speech;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
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

    private LocalBroadcastManager localBroadcastManager;

    private Instruction instruction;
    private String extraPrompt;

    private SpeechRecognizer speech;
    private String repondre;
    private String fermer;
    private String modifier;
    private String envoyer;

    public MySpeechRecorder(Context context){
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);

        this.repondre = context.getString(R.string.repondre);
        this.fermer = context.getString(R.string.fermer);
        this.modifier = context.getString(R.string.modifier);
        this.envoyer = context.getString(R.string.envoyer);

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
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent.putExtra(EXTRA_PARTIAL_RESULTS, true);
        }
        intent.putExtra(EXTRA_PROMPT, extraPrompt);
        intent.putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM);
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
        hideMicrophone();
    }

    public void onError(int error) {
        Log.d(TAG, "Error was thrown by SpeechRecognizer "+error);
        end(Collections.emptyList(), Activity.RESULT_CANCELED);
    }

    public void onResults(Bundle results) {
        Log.d(TAG, "onResults");
        end(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION), Activity.RESULT_OK);
    }

    private void end(List<String> words, int returnCode) {
        sendDestroy();
        hideMicrophone();
        onSpeechResult(instruction, returnCode, words);
    }

    private void sendDestroy() {
        localBroadcastManager.sendBroadcast(new Intent(SmsRecuActivity.Companion.getEVENT_DESTROY_SPEECH_RECOGNIZER()));
    }

    private void onSpeechResult(Instruction instruction, int returnCode, List<String> words) {
        Intent intent = new Intent(SmsRecuActivity.Companion.getEVENT_SPEECH_RESULT());
        intent.putExtra(SmsRecuActivity.Companion.getEXTRA_SPEECH_INSTRUCTION(), instruction);
        intent.putExtra(SmsRecuActivity.Companion.getEXTRA_SPEECH_RESULT_CODE(), returnCode);
        if(words instanceof ArrayList) {
            intent.putStringArrayListExtra(SmsRecuActivity.Companion.getEXTRA_SPEECH_WORDS(), (ArrayList<String>) words);
        } else {
            intent.putStringArrayListExtra(SmsRecuActivity.Companion.getEXTRA_SPEECH_WORDS(), new ArrayList<>());
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    private void hideMicrophone(){
        localBroadcastManager.sendBroadcast(new Intent(SmsRecuActivity.Companion.getEVENT_HIDE_MICROPHONE()));
    }

    public void destroy() {
        try {
            speech.setRecognitionListener(null);
            speech.stopListening();
            speech.destroy();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }
    }

    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> results = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(results==null) {
            return;
        }
        onPartialResult(results);
        String firstResult = results.get(0);
        switch (instruction) {
            case REPONDRE_FERMER:
                if(firstResult.equalsIgnoreCase(repondre)
                        || firstResult.equalsIgnoreCase(fermer)){
                    Log.d(TAG, "Premature end of recognization on partial result");
                    onResults(partialResults);
                }
                break;
            case MODIFIER_ENVOYER_FERMER:
                if(firstResult.equalsIgnoreCase(modifier)
                        || firstResult.equalsIgnoreCase(envoyer)
                        || firstResult.equalsIgnoreCase(fermer)){
                    Log.d(TAG, "Premature end of recognization on partial result");
                    onResults(partialResults);
                }
                break;
            default:break;
        }
    }

    private void onPartialResult(ArrayList<String> results) {
        Intent intent = new Intent(SmsRecuActivity.Companion.getEVENT_SPEECH_PARTIAL_RESULT());
        intent.putExtra(SmsRecuActivity.Companion.getEXTRA_SPEECH_WORDS(), results);
        localBroadcastManager.sendBroadcast(intent);
    }

    public void onEvent(int eventType, Bundle params) {

    }
}
