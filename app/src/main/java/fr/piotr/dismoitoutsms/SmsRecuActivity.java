package fr.piotr.dismoitoutsms;

import static android.telephony.PhoneStateListener.LISTEN_CALL_STATE;
import static android.view.View.*;
import static fr.piotr.dismoitoutsms.util.Diction.*;
import static fr.piotr.dismoitoutsms.util.Instruction.MODIFIER_ENVOYER_FERMER;
import static fr.piotr.dismoitoutsms.util.Instruction.REPONDRE_FERMER;
import static fr.piotr.dismoitoutsms.util.Instruction.REPONSE;

import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.messages.Message;
import fr.piotr.dismoitoutsms.reception.SmsReceiver;
import fr.piotr.dismoitoutsms.reception.TextToSpeechHelper;
import fr.piotr.dismoitoutsms.speech.MySpeechRecorder;
import fr.piotr.dismoitoutsms.util.AbstractActivity;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration;
import fr.piotr.dismoitoutsms.util.ContactHelper;
import fr.piotr.dismoitoutsms.util.Instruction;
import fr.piotr.dismoitoutsms.util.Sablier;
import fr.piotr.dismoitoutsms.util.WakeLockManager;

/**
 * @author Piotr
 * 
 */
public class SmsRecuActivity extends AbstractActivity {

	public static final String TAG = "SmsRecuActivity";

	public static final String EVENT_START_SPEECH_RECOGNIZER = "EVENT_START_SPEECH_RECOGNIZER";
	public static final String EVENT_FINISH = "EVENT_FINISH";

	public static final String EXTRA_INSTRUCTION = "EXTRA_INSTRUCTION";
	public static final String EXTRA_PROMPT = "EXTRA_PROMPT";

	private static final String		TELEPHON_NUMBER_FIELD_NAME	= "address";
	private static final String		MESSAGE_BODY_FIELD_NAME		= "body";
	private static final Uri		SENT_MSGS_CONTET_PROVIDER	= Uri.parse("content://sms/sent");
	private ImageView boutonRepeter;
	private ImageView boutonChut;
	private ImageView boutonRepondre;

	public enum Parameters {
		DATE, CONTACT_NAME, CONTACT, MESSAGE, NUMERO_A_QUI_REPONDRE
	}

    private Date date;
	private String							contactName;
	private Contact							contact;
	private String							message;
	private boolean							reconnaissanceInstallee;
	private String							reponse;
	private TextView						reponseMessage;
	private String							numeroAQuiRepondre;
	private ImageView						boutonEnvoyer;
	private LinearLayout					reponseLayout;
	private Sablier							sablier;
    private TextToSpeechHelper              speech;
    private MySpeechRecorder speechRecorder;
	private PhoneStateListener phoneStateListener;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch(intent.getAction()){
				case EVENT_FINISH:
					finish();
					break;
				case EVENT_START_SPEECH_RECOGNIZER:
					startSpeechRecognizer((Instruction) intent.getSerializableExtra(EXTRA_INSTRUCTION), intent.getStringExtra(EXTRA_PROMPT));
			}
		}
	};

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		WakeLockManager.setWakeUp(this);

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		SmsReceiver.getInstance().setDictating(true);

		Bundle extras = getIntent().getExtras();
        date = new Date(extras.getLong(Parameters.DATE.name()));
		contactName = extras.getString(Parameters.CONTACT_NAME.toString());
		contact = (Contact) extras.getSerializable(Parameters.CONTACT.toString());
		message = extras.getString(Parameters.MESSAGE.toString());
		numeroAQuiRepondre = extras.getString(Parameters.NUMERO_A_QUI_REPONDRE.toString());
		setContentView(R.layout.smsrecudialog);

        ImageView photo = (ImageView) findViewById(R.id.smsrecucontactphoto);
        if(contact!=null && contact.hasAPhoto()){
            photo.setImageBitmap(ContactHelper.getPhotoContact(this, contact.getPhotoId()));
        }

        detecterSiReconnaissanceVocaleInstallee();

		setTitle(contactName);

		initExpediteur();
		initMessage();
		initBoutons();
		initReponse();

		sablier = new Sablier(this);
        final TextView sablierIndicator = (TextView) findViewById(R.id.sablierIndicator);
        sablier.setOnProgressListener(value -> runOnUiThread(() -> sablierIndicator.setText(String.valueOf(value))));
		sablier.start();

        speech = new TextToSpeechHelper(this, () -> {
            String text = contactName + " " + getString(R.string.dit) + " " + message;
            speech.parler(text, MESSAGE_RECU);
        });

		phoneStateListener = new PhoneStateListener() {

            int lastState=-1;

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				super.onCallStateChanged(state, incomingNumber);
				switch (state) {
					case TelephonyManager.CALL_STATE_IDLE:
//                        // Téléphone inactif
//                        if(lastState==TelephonyManager.CALL_STATE_RINGING
//                                || lastState==TelephonyManager.CALL_STATE_OFFHOOK){
//
//                            new Timer().schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    sablier.unFreez();
//                                    sablier.reset();
//                                    speech.parler(message, MESSAGE_RECU);
//                                }
//                            }, 2000);
//
//                        }
						break;
					case TelephonyManager.CALL_STATE_RINGING:
                        // Le téléphone sonne
                    case TelephonyManager.CALL_STATE_OFFHOOK:
//                        // Conversation en cours
//                        speech.stopLecture();
//                        sablier.freez();
                        SmsReceiver.getInstance().standBy(new Message(date, contact, message));
                        finish();
                        break;
					default:
						Log.i(TAG, "UNKNOWN_STATE: " + state);
						break;
				}
                lastState = state;
			}
		};


	}

	@Override
	protected void onResume() {
		super.onResume();

//		EventAnnotationManager.registerReceiver(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(EVENT_FINISH);
		filter.addAction(EVENT_START_SPEECH_RECOGNIZER);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

//        instance = this;
		SmsReceiver.getInstance().setDictating(true);

		boutonRepeter.setOnClickListener(v -> {
            speech.parler(message, MESSAGE_RECU);
            sablier.reset();
        });

		boutonChut.setOnClickListener(v -> {
            speech.stopLecture();
            sablier.reset();
        });

		boutonRepondre.setEnabled(isReconnaissanceInstallee()
                && ConfigurationManager.getBoolean(SmsRecuActivity.this,
                Configuration.COMMANDE_VOCALE));
		boutonRepondre.setOnClickListener(v -> {
            startSpeechRecognizer(REPONSE, getString(R.string.reponse));
            sablier.reset();
        });

		boutonEnvoyer.setEnabled(reponse != null && !reponse.isEmpty()
                && numeroAQuiRepondre != null && !numeroAQuiRepondre.isEmpty());
		boutonEnvoyer.setOnClickListener(v -> envoyer());

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, LISTEN_CALL_STATE);

	}

	@Override
	protected void onPause() {
		super.onPause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		SmsReceiver.getInstance().setDictating(false);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
//		EventAnnotationManager.unregisterReceiver(this);

		boutonRepeter.setOnClickListener(null);
		boutonChut.setOnClickListener(null);
		boutonRepondre.setOnClickListener(null);
		boutonEnvoyer.setOnClickListener(null);


        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

    }

    @Override
    public void finish() {
        super.finish();
        end();
    }

    @Override
    public boolean moveTaskToBack(boolean nonRoot) {
        finish();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_HOME) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void end() {
//        instance = null;
		SmsReceiver.getInstance().setDictating(false);
        speech.stopLecture();
        speech.shutdown();
        sablier.finished();
        if(speechRecorder!=null){
            speechRecorder.stop();
        }
        SmsReceiver.getInstance().nextMessage(this);
    }

	public void initMessage() {
		text(R.id.message).setText(message);
	}

	public void initReponse() {
		reponseLayout = (LinearLayout) findViewById(R.id.reponseLayout);
		reponseLayout.setVisibility(INVISIBLE);
		reponseMessage = (TextView) findViewById(R.id.reponsemessagetxt);
	}

	public void initExpediteur() {
		if (contact != null) {
			TextView contactName = (TextView) findViewById(R.id.contactname);
			contactName.setText(contact.getName());
		}
	}

	public void initBoutons() {
		initBoutonRepeter();
		initBoutonChut();
		initBoutonRepondre();
		initBoutonEnvoyer();
	}

	public void initBoutonEnvoyer() {
		boutonEnvoyer = (ImageView) findViewById(R.id.envoyerBtn);
	}

	public void envoyer() {
		if (numeroAQuiRepondre != null) {
			SmsManager.getDefault().sendTextMessage(numeroAQuiRepondre, null, reponse, null, null);
			reponseMessage.setText(getString(R.string.messageenvoye));
			boutonEnvoyer.setEnabled(false);
			Toast.makeText(SmsRecuActivity.this,
					getString(R.string.vousavezrepondu) + " " + reponse, Toast.LENGTH_LONG).show();
			addMessageToSent(numeroAQuiRepondre, reponse);
		}
		finish();
	}

	public void initBoutonRepondre() {
		boutonRepondre = (ImageView) findViewById(R.id.repondre);
	}

	public void initBoutonChut() {
		boutonChut = (ImageView) findViewById(R.id.chut);
	}

	public void initBoutonRepeter() {
		boutonRepeter = (ImageView) findViewById(R.id.repeter);
	}

	/**
	 * Permet d'ajouter le SMS envoyé dans la conversation
	 * 
	 */
	void addMessageToSent(String telNumber, String messageBody) {
		ContentValues sentSms = new ContentValues();
		sentSms.put(TELEPHON_NUMBER_FIELD_NAME, telNumber);
		sentSms.put(MESSAGE_BODY_FIELD_NAME, messageBody);

		ContentResolver contentResolver = getContentResolver();
		contentResolver.insert(SENT_MSGS_CONTET_PROVIDER, sentSms);
	}

	public void detecterSiReconnaissanceVocaleInstallee() {
		PackageManager packageManager = getPackageManager();
		final List<ResolveInfo> activities = packageManager.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		reconnaissanceInstallee = activities.size() > 0;
	}

//	/**
//	 * Fire an intent to start the voice recognition activity.
//	 */
//	public static void startVoiceRecognition(final Instruction instruction, final String extraPrompt) {
//        if(SmsReceiver.getInstance().isDictating()) {
//            startSpeechRecognizer(instruction, extraPrompt);
//        }
//	}

    private void startSpeechRecognizer(final Instruction instruction, final String extraPrompt) {
        sablier.reset();

        runOnUiThread(() -> {
            showMicrophone();
            TextView speechInstructions = (TextView) findViewById(R.id.speech_instructions);
            speechInstructions.setText(extraPrompt);
            TextView reponseEnCours = (TextView) findViewById(R.id.reponse_en_cours);
            reponseEnCours.setText("");
            speechRecorder = new MySpeechRecorder(SmsRecuActivity.this);
            speechRecorder.startListening(instruction, extraPrompt);
        });
    }

    private boolean instructionIs(List<String> instructions, String... possibilities) {
		for (String instruction : instructions) {
			String lowerInstruction = instruction.toLowerCase();
			for (String possibility : possibilities) {
				if (lowerInstruction.contains(possibility.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	public void onPartialResult(final List<String> words){
        runOnUiThread(() -> {
            TextView reponseEnCours = (TextView) findViewById(R.id.reponse_en_cours);
            reponseEnCours.setText(words.get(0));
        });
	}

    public void onSpeechResult(Instruction instruction, int resultCode, List<String> words) {
        sablier.reset();
        if (instruction.is(REPONDRE_FERMER,MODIFIER_ENVOYER_FERMER) && resultCode == RESULT_OK) {

			if (instructionIs(words, getString(R.string.repondre), getString(R.string.modifier))) {
				startSpeechRecognizer(REPONSE, getString(R.string.reponse));
			} else if (instructionIs(words, getString(R.string.envoyer))) {
				envoyer();
				speech.parler(getString(R.string.messageenvoye), MESSAGE_ENVOYE);
			} else if (instructionIs(words, getString(R.string.fermer))) {
				onBackPressed();
			}

		} else if (instruction.is(REPONSE) && resultCode == RESULT_OK) {
			reponse = words.get(0);
			reponseLayout.setVisibility(VISIBLE);
			reponseMessage.setText(reponse);
			boutonEnvoyer.setEnabled(reponse != null && !reponse.isEmpty()
					&& numeroAQuiRepondre != null && !numeroAQuiRepondre.isEmpty());
			speech.parler(getString(R.string.votrereponseest) + reponse, VOUS_AVEZ_REPONDU);

		}
    }

    public void hideMicrophone() {
        View micophone = findViewById(R.id.microphone);
        micophone.setVisibility(GONE);
    }

    private void showMicrophone() {
        findViewById(R.id.microphone).setVisibility(VISIBLE);
    }

    @Override
	public void onBackPressed() {
		super.onBackPressed();
//		instance = null;
		SmsReceiver.getInstance().setDictating(false);
		sablier.finished();
	}

	/**
	 * @return the reconnaissanceInstallee
	 */
	public boolean isReconnaissanceInstallee() {
		return reconnaissanceInstallee;
	}

	@Override
	protected void onDestroy() {
//		instance = null;
		SmsReceiver.getInstance().setDictating(false);
		sablier.finished();
		super.onDestroy();
	}

}
