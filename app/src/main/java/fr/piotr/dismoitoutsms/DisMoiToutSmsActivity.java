package fr.piotr.dismoitoutsms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;
import fr.piotr.dismoitoutsms.util.AbstractActivity;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

import static android.media.AudioManager.FLAG_PLAY_SOUND;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_MUSIC;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.ARRET_STEP_DETECTOR;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.COMMANDE_VOCALE;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.EMOTICONES;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.setBoolean;

public class DisMoiToutSmsActivity extends AbstractActivity {

    public static final String EVENT_ACTIVATE = "EVENT_ACTIVATE";
    public static final String EVENT_DEACTIVATE = "EVENT_DEACTIVATE";

    public static final String TAG = "DisMoiToutSmsActivity";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case EVENT_ACTIVATE:
                    onActivate();
                    break;
                case EVENT_DEACTIVATE:
                    onDeactivate();
                    break;
            }
        }
    };

    private IntentFilter filter = new IntentFilter(){{
        addAction(EVENT_ACTIVATE);
        addAction(EVENT_DEACTIVATE);
    }};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.drawer_layout);

		verifierExistanceServiceSyntheseVocale();

		initVolumeControl();

        //Drawer

        initLanguageChooser();

        iniContactsControls();

        initEmoticonesControl();

        initBoutonTester();

        initBoutonReponseVocale();

        initStepDetectorOption();

	}

	@SuppressLint("RtlHardcoded")
    @Override
	protected void onResume() {
		super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        findViewById(R.id.menu).setOnClickListener(v -> {
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawerLayout.openDrawer(Gravity.LEFT);
        });

		SeekBar volumeSeek = (SeekBar) findViewById(R.id.volumeSeekTab);
		volumeSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.setStreamVolume(STREAM_MUSIC, progress, FLAG_PLAY_SOUND);
            }
        });

        toggleStatus(false);
        ImageView statusIcon = (ImageView) findViewById(R.id.status_icon);
        statusIcon.setOnClickListener(v -> {
            Intent intent = new Intent(DisMoiToutSmsActivity.this, ServiceCommunicator.class);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            if (isMyServiceRunning()) {
                stopService(intent);
            } else {
                // Volume à 100% à l'activation
                final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.setStreamVolume(STREAM_MUSIC, 100, FLAG_PLAY_SOUND | FLAG_SHOW_UI);
                startService(intent);
            }
            toggleStatus(true);
        });

        TextView btnActivate = (TextView)findViewById(R.id.btn_activate);
        btnActivate.setOnClickListener(v -> {
            onActivate();
        });

        TextView btnDeactivate = (TextView)findViewById(R.id.btn_deactivate);
        btnDeactivate.setOnClickListener(v -> {
            onDeactivate();
        });

        //Drawer
        languageChooser().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                CharSequence locale = ((TextView) arg1).getText();
                ConfigurationManager.setLangueSelectionnee(getApplicationContext(),
                        locale.toString());
//                TextToSpeechHelper.changerLaLangue(getApplicationContext());
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }

        });

        checkBoxReponseVocale().setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(checkPermissions(PERMISSIONS_RECORD_AUDIO, Manifest.permission.RECORD_AUDIO)) {
                setBoolean(getApplicationContext(), COMMANDE_VOCALE, isChecked);
            }
        });

        btnTester().setOnClickListener(v -> launchTest());

        checkBoxEmoticones()
                .setOnCheckedChangeListener((buttonView, isChecked) -> setBoolean(DisMoiToutSmsActivity.this, EMOTICONES,
                        isChecked));

        checkBoxUniquementMesContacts().setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(checkPermissions(PERMISSIONS_REQUEST_READ_CONTACTS, Manifest.permission.READ_CONTACTS)) {
                setBoolean(getApplicationContext(), UNIQUEMENT_CONTACTS,
                        isChecked);
                if (isChecked) {
                    startActivity(new Intent(DisMoiToutSmsActivity.this, ContactSelectionActivity.class));
                }
            }
        });

        if(isKitKatWithStepCounter()) {
            checkBoxStepDetector().setOnCheckedChangeListener((buttonView, isChecked) -> setBoolean(getApplicationContext(), ARRET_STEP_DETECTOR, isChecked));
        }

        findViewById(R.id.parametreTextViewUniquementMesContacts).setOnClickListener(this::openContactSelection);
        findViewById(R.id.choisirContactsTab).setOnClickListener(this::openContactSelection);

        checkPermissions(PERMISSIONS_REQUEST_RESUME,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE);

	}

    public void onActivate() {
        if(!isMyServiceRunning()){
            Intent intent = new Intent(DisMoiToutSmsActivity.this, ServiceCommunicator.class);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startService(intent);
            toggleStatus(true);
        }
    }

    public void onDeactivate() {
        if(isMyServiceRunning()){
            Intent intent = new Intent(DisMoiToutSmsActivity.this, ServiceCommunicator.class);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            stopService(intent);
            toggleStatus(true);
        }
    }

    private void launchTest() {
        if(isMyServiceRunning()) {
            String contact = getString(R.string.app_name);
            String message = getString(R.string.test_diction);

//            TextToSpeechHelper.parler(this, message, MESSAGE_RECU);
            Intent intent = new Intent(DisMoiToutSmsActivity.this, SmsRecuActivity.class);
            intent.putExtra(SmsRecuActivity.Parameters.DATE.name(), new Date().getTime());
            intent.putExtra(SmsRecuActivity.Parameters.CONTACT_NAME.toString(), contact);
            intent.putExtra(SmsRecuActivity.Parameters.MESSAGE.toString(), message);
            intent.putExtra(SmsRecuActivity.Parameters.CONTACT.name(), new Contact(-1, contact, "0000000000", null));
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.deactivated, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleStatus(boolean animate) {
        if(animate){
            toggleStatusAnimated();
        } else {
            toggleStatusSimple();
        }
    }

    @SuppressWarnings("deprecation")
    private int getResolvedColor(int resId){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(resId, getTheme());
        } else
            return getResources().getColor(resId);
    }

    private void toggleStatusAnimated() {
        final ImageView statusIcon = (ImageView) findViewById(R.id.status_icon);
        Animation retrecissement = AnimationUtils.loadAnimation(this, R.anim.retrecissement);
        retrecissement.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {

            }

            public void onAnimationEnd(Animation animation) {
                TextView statusText = (TextView) findViewById(R.id.status_text);
                if(isMyServiceRunning()) {
                    statusIcon.setImageResource(R.drawable.ic_activate_48dp);
                    statusText.setText(getString(R.string.activated));
                    statusText.setTextColor(getResolvedColor(R.color.green500));
                } else {
                    statusIcon.setImageResource(R.drawable.ic_deactivate_48dp);
                    statusText.setText(getString(R.string.deactivated));
                    statusText.setTextColor(getResolvedColor(R.color.red500));
                }
                Animation agrandissement = AnimationUtils.loadAnimation(DisMoiToutSmsActivity.this, R.anim.agrandissement);
                statusIcon.startAnimation(agrandissement);
            }

            public void onAnimationRepeat(Animation animation) {

            }
        });
        statusIcon.startAnimation(retrecissement);
    }

    private void toggleStatusSimple() {
        ImageView statusIcon = (ImageView) findViewById(R.id.status_icon);
        TextView statusText = (TextView) findViewById(R.id.status_text);
        if(isMyServiceRunning()) {
            statusIcon.setImageResource(R.drawable.ic_activate_48dp);
            statusText.setText(getString(R.string.activated));
            statusText.setTextColor(getResolvedColor(R.color.green500));
        } else {
            statusIcon.setImageResource(R.drawable.ic_deactivate_48dp);
            statusText.setText(getString(R.string.deactivated));
            statusText.setTextColor(getResolvedColor(R.color.red500));
        }
    }

    @Override
	protected void onPause() {
		super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

		SeekBar volumeSeek = (SeekBar) findViewById(R.id.volumeSeekTab);
		volumeSeek.setOnSeekBarChangeListener(null);

        ImageView statusIcon = (ImageView) findViewById(R.id.status_icon);
        statusIcon.setOnClickListener(null);

        TextView btnActivate = (TextView)findViewById(R.id.btn_activate);
        btnActivate.setOnClickListener(null);

        TextView btnDeactivate = (TextView)findViewById(R.id.btn_deactivate);
        btnDeactivate.setOnClickListener(null);

        //Drawer
        languageChooser().setOnItemSelectedListener(null);
        btnTester().setOnClickListener(null);
        checkBoxReponseVocale().setOnCheckedChangeListener(null);
        checkBoxEmoticones().setOnCheckedChangeListener(null);
        checkBoxUniquementMesContacts().setOnCheckedChangeListener(null);
        checkBoxStepDetector().setOnCheckedChangeListener(null);
        findViewById(R.id.parametreTextViewUniquementMesContacts).setOnClickListener(null);
        findViewById(R.id.choisirContactsTab).setOnClickListener(null);
	}

	public void initVolumeControl() {
		// Volume

		setVolumeControlStream(STREAM_MUSIC);

		final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		SeekBar volumeSeek = (SeekBar) findViewById(R.id.volumeSeekTab);
		volumeSeek.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		volumeSeek.setProgress(audioManager.getStreamVolume(STREAM_MUSIC));
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			SeekBar volumeSeek = (SeekBar) findViewById(R.id.volumeSeekTab);
			final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			volumeSeek.setProgress(audioManager.getStreamVolume(STREAM_MUSIC));
		}
		return super.onKeyUp(keyCode, event);
	}

    public void verifierExistanceServiceSyntheseVocale() {
        try {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, 0x01);
        } catch (ActivityNotFoundException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0x01) {
			if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// Echec, aucun moteur n'a été trouvé, on propose à
				// l'utilisateur d'en installer un depuis le Market
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                try {
                    startActivity(installIntent);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
			}
		}
	}

    //Drawer

    private CheckBox checkBoxUniquementMesContacts() {
        return checkbox(R.id.uniquementContactesTab);
    }

    private CheckBox checkBoxEmoticones() {
        return checkbox(R.id.translateEmoticonesBtnTab);
    }

    private CheckBox checkBoxReponseVocale() {
        return checkbox(R.id.commandeVocaleBtnTab);
    }

    private Button btnTester() {
        return button(R.id.testerDictionBtnTab);
    }

    private Spinner languageChooser() {
        return spinner(R.id.languageChooserTab);
    }

    private void initLanguageChooser() {
        Spinner languageChooser = languageChooser();
        List<String> list = new ArrayList<>();
        Locale[] availableLocales = Locale.getAvailableLocales();
        Arrays.sort(availableLocales, (object1, object2) -> object1.getDisplayName().compareTo(object2.getDisplayName()));
        Locale langueSelectionnee = ConfigurationManager.getLangueSelectionnee(this);
        int i = 0;
        int selectedPosition = 0;
        for (Locale locale : availableLocales) {
            list.add(locale.getDisplayName());
            if (locale.equals(langueSelectionnee)) {
                selectedPosition = i;
            }
            i++;
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageChooser.setAdapter(dataAdapter);
        languageChooser.setSelection(selectedPosition);

    }

    public void initBoutonReponseVocale() {
        checkBoxReponseVocale().setChecked(getBoolean(this, COMMANDE_VOCALE));
    }

    public void initBoutonTester() {
        btnTester().setEnabled(true);
    }

    public void initEmoticonesControl() {
        checkBoxEmoticones().setChecked(getBoolean(this, EMOTICONES));
    }

    public void iniContactsControls() {
        checkBoxUniquementMesContacts().setChecked(getBoolean(this, UNIQUEMENT_CONTACTS));
    }

    private void initStepDetectorOption() {
        if(isKitKatWithStepCounter()) {
            checkBoxStepDetector().setChecked(getBoolean(this, ARRET_STEP_DETECTOR));
        } else {
            findViewById(R.id.card_step_detector).setVisibility(View.GONE);
        }
    }

    public boolean isKitKatWithStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PackageManager pm = getPackageManager();
            return pm.hasSystemFeature (PackageManager.FEATURE_SENSOR_STEP_DETECTOR)
                    && pm.hasSystemFeature (PackageManager.FEATURE_SENSOR_STEP_COUNTER);
        }
        return false;
    }

    private CheckBox checkBoxStepDetector() {
        return checkbox(R.id.stepDetectorCheckBox);
    }

    public void openPrivacyPolicy(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://objectifandroid.blogspot.fr/2016/12/politique-de-confidentialite.html"));
        startActivity(intent);
    }
}