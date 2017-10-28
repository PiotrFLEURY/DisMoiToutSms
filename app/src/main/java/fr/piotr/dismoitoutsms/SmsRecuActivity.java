package fr.piotr.dismoitoutsms;

import android.app.Activity;
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
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.contacts.Contacts;
import fr.piotr.dismoitoutsms.dialogs.ContactSelectionDialog;
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

import static android.telephony.PhoneStateListener.LISTEN_CALL_STATE;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static fr.piotr.dismoitoutsms.util.Diction.MESSAGE_ENVOYE;
import static fr.piotr.dismoitoutsms.util.Diction.MESSAGE_RECU;
import static fr.piotr.dismoitoutsms.util.Diction.VOUS_AVEZ_REPONDU;
import static fr.piotr.dismoitoutsms.util.Instruction.DICTER_CONTACT;
import static fr.piotr.dismoitoutsms.util.Instruction.MODIFIER_ENVOYER_FERMER;
import static fr.piotr.dismoitoutsms.util.Instruction.REPONDRE_FERMER;
import static fr.piotr.dismoitoutsms.util.Instruction.REPONSE;

/**
 * @author Piotr
 * 
 */
public class SmsRecuActivity extends AbstractActivity {

	public static final String TAG = "SmsRecuActivity";

	public static final String EXTRA_SPEECH_WORDS = TAG + ".EXTRA_SPEECH_WORDS";
	public static final String EXTRA_SPEECH_RESULT_CODE = TAG + ".EXTRA_SPEECH_RESULT_CODE";
	public static final String EXTRA_SPEECH_INSTRUCTION = TAG + ".EXTRA_SPEECH_INSTRUCTION";
	public static final String EVENT_SPEECH_RESULT = TAG + ".EVENT_SPEECH_RESULT";

	public static final String EVENT_SPEECH_PARTIAL_RESULT = TAG + ".EVENT_SPEECH_PARTIAL_RESULT";

	public static final String EVENT_DESTROY_SPEECH_RECOGNIZER = TAG + ".EVENT_DESTROY_SPEECH_RECOGNIZER";

	public static final String EVENT_HIDE_MICROPHONE = TAG + ".EVENT_HIDE_MICROPHONE";

	public static final String EVENT_START_SPEECH_RECOGNIZER = TAG + ".EVENT_START_SPEECH_RECOGNIZER";
	public static final String EVENT_FINISH = TAG + ".EVENT_FINISH";
	public static final String EVENT_BACK = TAG + ".EVENT_BACK";

	public static final String EXTRA_INSTRUCTION = TAG + ".EXTRA_INSTRUCTION";
	public static final String EXTRA_PROMPT = TAG + ".EXTRA_PROMPT";

	private static final String		TELEPHON_NUMBER_FIELD_NAME	= "address";
	private static final String		MESSAGE_BODY_FIELD_NAME		= "body";
	private static final Uri		SENT_MSGS_CONTET_PROVIDER	= Uri.parse("content://sms/sent");

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
	private Sablier							sablier;
    private TextToSpeechHelper              speech;
    private MySpeechRecorder speechRecorder;
	private PhoneStateListener phoneStateListener;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch(intent.getAction()){
				case EVENT_BACK:
					onBackPressed();
					break;
				case EVENT_FINISH:
					finish();
					break;
				case EVENT_START_SPEECH_RECOGNIZER:
					startSpeechRecognizer((Instruction) intent.getSerializableExtra(EXTRA_INSTRUCTION),
							intent.getStringExtra(EXTRA_PROMPT));
					break;
				case EVENT_HIDE_MICROPHONE:
					hideMicrophone();
					break;
				case EVENT_SPEECH_RESULT:
					onSpeechResult((Instruction) intent.getSerializableExtra(EXTRA_SPEECH_INSTRUCTION),
							intent.getIntExtra(EXTRA_SPEECH_RESULT_CODE, -1),
							intent.getStringArrayListExtra(EXTRA_SPEECH_WORDS));
					break;
				case EVENT_SPEECH_PARTIAL_RESULT:
					onPartialResult(intent.getStringArrayListExtra(EXTRA_SPEECH_WORDS));
					break;
				case EVENT_DESTROY_SPEECH_RECOGNIZER:
					destroySpeechRecognizer();
					break;
				case ContactSelectionDialog.EVENT_CONTACT_SELECTED:
					Contact contact = (Contact) intent.getSerializableExtra(ContactSelectionDialog.EXTRA_CONTACT_SELECTED);
					onContactSelected(contact);
					break;
			}
		}
	};

	private void destroySpeechRecognizer() {
		runOnUiThread(() -> speechRecorder.destroy());
	}

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

		initPhoto();

        detecterSiReconnaissanceVocaleInstallee();

		setTitle(contactName);

		initExpediteur();
		initMessage();
		initReponse();

		sablier = new Sablier(this);
        final TextView sablierIndicator = findViewById(R.id.sablierIndicator);
        sablier.setOnProgressListener(value -> runOnUiThread(() -> sablierIndicator.setText(String.valueOf(value))));
		sablier.start();

        speech = new TextToSpeechHelper(this, () -> {
			if(message!=null) {
				String text = contactName + " " + getString(R.string.dit) + " " + message;
				speech.parler(text, MESSAGE_RECU);
			} else {
				askForContact();
			}
        });

		phoneStateListener = new PhoneStateListener() {

            int lastState=-1;

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				super.onCallStateChanged(state, incomingNumber);
				switch (state) {
					case TelephonyManager.CALL_STATE_IDLE:
						break;
					case TelephonyManager.CALL_STATE_RINGING:
                        // Le téléphone sonne
                    case TelephonyManager.CALL_STATE_OFFHOOK:
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

	private void askForContact() {
		startSpeechRecognizer(Instruction.DICTER_CONTACT, getString(R.string.dictate_contact_name));
	}

	private void initPhoto() {
		ImageView photo = findViewById(R.id.smsrecucontactphoto);
		if(contact!=null && contact.hasAPhoto()){
            photo.setImageBitmap(ContactHelper.getPhotoContact(this, contact.getPhotoId()));
        }
	}

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(EVENT_FINISH);
		filter.addAction(EVENT_START_SPEECH_RECOGNIZER);
		filter.addAction(EVENT_BACK);
		filter.addAction(EVENT_HIDE_MICROPHONE);
		filter.addAction(EVENT_SPEECH_RESULT);
		filter.addAction(EVENT_SPEECH_PARTIAL_RESULT);
		filter.addAction(EVENT_DESTROY_SPEECH_RECOGNIZER);
		filter.addAction(ContactSelectionDialog.EVENT_CONTACT_SELECTED);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

		SmsReceiver.getInstance().setDictating(true);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, LISTEN_CALL_STATE);

	}

	private void repondre() {
		startSpeechRecognizer(REPONSE, getString(R.string.reponse));
		sablier.reset();
	}

	private void stop() {
		speech.stopLecture();
		sablier.reset();
	}

	private void repeter() {
		if(message!=null) {
			speech.parler(message, MESSAGE_RECU);
			sablier.reset();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		SmsReceiver.getInstance().setDictating(false);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

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
		SmsReceiver.getInstance().setDictating(false);
        speech.stopLecture();
        speech.shutdown();
        sablier.finished();
        if(speechRecorder!=null){
            destroySpeechRecognizer();
        }
        SmsReceiver.getInstance().nextMessage(this);
    }

	public void initMessage() {
		TextView tvMessage = text(R.id.message);
		if(message==null){
			tvMessage.setVisibility(GONE);
		} else {
			tvMessage.setText(message);
		}
	}

	public void initReponse() {
		reponseMessage = findViewById(R.id.reponsemessagetxt);
		reponseMessage.setVisibility(INVISIBLE);
	}

	public void initExpediteur() {
		if (contact != null) {
			TextView contactName = findViewById(R.id.contactname);
			contactName.setText(contact.getName());
		}
	}

	public void envoyer() {
		//TODO gérer le cas du destinataire ou message vide
		if (!TextUtils.isEmpty(numeroAQuiRepondre) && !TextUtils.isEmpty(reponse)) {
			SmsManager.getDefault().sendTextMessage(numeroAQuiRepondre, null, reponse, null, null);
			reponseMessage.setText(getString(R.string.messageenvoye));
			invalidateOptionsMenu();
			Toast.makeText(SmsRecuActivity.this,
					getString(R.string.vousavezrepondu) + " " + reponse, Toast.LENGTH_LONG).show();
			addMessageToSent(numeroAQuiRepondre, reponse);
		}
		finish();
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

    private void startSpeechRecognizer(final Instruction instruction, final String extraPrompt) {
        sablier.reset();

        runOnUiThread(() -> {
            showMicrophone();
            TextView speechInstructions = findViewById(R.id.speech_instructions);
            speechInstructions.setText(extraPrompt);
            TextView reponseEnCours = findViewById(R.id.reponse_en_cours);
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

	private void onPartialResult(final List<String> words){
        runOnUiThread(() -> {
            TextView reponseEnCours = findViewById(R.id.reponse_en_cours);
            reponseEnCours.setText(words.get(0));
        });
	}

    private void onSpeechResult(Instruction instruction, int resultCode, List<String> words) {
        sablier.reset();
		if(Activity.RESULT_CANCELED == resultCode){
			Snackbar.make(findViewById(R.id.smsrecu_coordinator), R.string.error_occured, Snackbar.LENGTH_INDEFINITE)
			.setAction(R.string.action_retry, v -> repondre())
			.show();
		} else if (instruction.is(REPONDRE_FERMER,MODIFIER_ENVOYER_FERMER) && resultCode == RESULT_OK) {

			if (instructionIs(words, getString(R.string.repondre), getString(R.string.modifier))) {
				startSpeechRecognizer(REPONSE, getString(R.string.reponse));
			} else if (instructionIs(words, getString(R.string.envoyer))) {
				envoyer();
				speech.parler(getString(R.string.messageenvoye), MESSAGE_ENVOYE);
			} else if (instructionIs(words, getString(R.string.fermer))) {
				LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(EVENT_BACK));
			}

		} else if (instruction.is(REPONSE) && resultCode == RESULT_OK) {
			reponse = words.get(0);
			reponseMessage.setVisibility(VISIBLE);
			reponseMessage.setText(reponse);
			ScrollView scrollView = findViewById(R.id.smsrecu_scrollview);
			scrollView.fullScroll(View.FOCUS_DOWN);
			invalidateOptionsMenu();
			speech.parler(getString(R.string.votrereponseest) + reponse, VOUS_AVEZ_REPONDU);

		} else if(instruction.is(DICTER_CONTACT)){
			if(resultCode == RESULT_OK) {
				String result = words.get(0);
				Contacts correspondances = getCorrespondance(result);
				ContactSelectionDialog contactSelectionDialog = new ContactSelectionDialog(this);
				contactSelectionDialog.setContacts(correspondances);
				contactSelectionDialog.show();
			} else {
				onBackPressed();
			}
		}
    }

    private void onContactSelected(Contact contact){
		listenForNewMessage(contact);
	}

	private void listenForNewMessage(Contact contact) {
		this.contact = contact;
		contactName = this.contact.getName();
		numeroAQuiRepondre = this.contact.getTelephone();
		initPhoto();
		setTitle(contactName);
		initExpediteur();
		startSpeechRecognizer(REPONSE, getString(R.string.reponse));
	}

	private Contacts getCorrespondance(String result) {
		Contacts correspondances = new Contacts();
		for (Contact aContact : ContactHelper.getAllContacts()) {
			if(aContact.getName().contains(result)){
				correspondances.add(aContact);
			}
		}
		return correspondances;
	}

	private void hideMicrophone() {
        findViewById(R.id.microphone).setVisibility(GONE);
    }

    private void showMicrophone() {
        findViewById(R.id.microphone).setVisibility(VISIBLE);
    }

    @Override
	public void onBackPressed() {
		super.onBackPressed();
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
		SmsReceiver.getInstance().setDictating(false);
		sablier.finished();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.sms_recu_menu, menu);

		menu.findItem(R.id.action_answer).setEnabled(isReconnaissanceInstallee()
				&& ConfigurationManager.getBoolean(SmsRecuActivity.this,
				Configuration.COMMANDE_VOCALE));

		menu.findItem(R.id.action_send).setEnabled(reponse != null && !reponse.isEmpty()
				&& numeroAQuiRepondre != null && !numeroAQuiRepondre.isEmpty());

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.action_repeat:
				repeter();
				break;
			case R.id.action_stop:
				stop();
				break;
			case R.id.action_answer:
				repondre();
				break;
			case R.id.action_send:
				envoyer();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

}
