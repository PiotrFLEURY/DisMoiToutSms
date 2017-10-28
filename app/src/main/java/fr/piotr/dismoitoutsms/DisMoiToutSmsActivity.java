package fr.piotr.dismoitoutsms;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService;
import fr.piotr.dismoitoutsms.util.AbstractActivity;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

import static android.media.AudioManager.FLAG_PLAY_SOUND;
import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_MUSIC;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.ARRET_STEP_DETECTOR;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.COMMANDE_VOCALE;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.EMOTICONES;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.HEADSET_MODE;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.setBoolean;

public class DisMoiToutSmsActivity extends AbstractActivity {

    public static final String TAG = "DisMoiToutSmsActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_24dp);
        }

        setContentView(R.layout.drawer_layout_v4);

        if(!isMyServiceRunning(DisMoiToutSmsService.class)){
            startService(new Intent(this, DisMoiToutSmsService.class));
        }

		verifierExistanceServiceSyntheseVocale();

		initVolumeControl();

        initLanguageChooser();

        iniContactsControls();

        initEmoticonesControl();

        initBoutonTester();

        initBoutonReponseVocale();

        initStepDetectorOption();

        initHeadSetOption();

	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                toggleDrawer();
                break;
            case R.id.main_menu_people:
                openContactSelection(null);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
	protected void onResume() {
		super.onResume();

        toggleStatus();

        Switch switchActivation = findViewById(R.id.switch_activation);
        switchActivation.setChecked(isMyServiceRunning());
        switchActivation.setOnClickListener(v -> {
            if(((Switch)v).isChecked()){
                onActivate();
            } else {
                onDeactivate();
            }
        });

        //Drawer
        languageChooser().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if(arg1!=null) {
                    CharSequence locale = ((TextView) arg1).getText();
                    if(locale!=null) {
                        ConfigurationManager.setLangueSelectionnee(getApplicationContext(),
                                locale.toString());
                    }
                }
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

        checkBoxHeadSetMode().setOnCheckedChangeListener((buttonView, isChecked) -> setBoolean(getApplicationContext(), HEADSET_MODE, isChecked));

        findViewById(R.id.gererContacts).setOnClickListener(this::openContactSelection);

        checkPermissions(PERMISSIONS_REQUEST_RESUME,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_PHONE_STATE);

	}

    private void setupVolume() {
        // Volume à 100% à l'activation
        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int halfVolume = maxVolume / 2;
        if(currentVolume < halfVolume){
            audioManager.setStreamVolume(STREAM_MUSIC, halfVolume, FLAG_PLAY_SOUND | FLAG_SHOW_UI);
        }
    }

    private void toggleDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if(drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            drawerLayout.openDrawer(Gravity.START);
        }
    }

    public void onActivate() {
        if(!isMyServiceRunning()){
            Intent intent = new Intent(DisMoiToutSmsActivity.this, ServiceCommunicator.class);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            setupVolume();
            startService(intent);
            toggleStatus();
        }
    }

    public void onDeactivate() {
        if(isMyServiceRunning()){
            Intent intent = new Intent(DisMoiToutSmsActivity.this, ServiceCommunicator.class);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            stopService(intent);
            toggleStatus();
        }
    }

    private void launchTest() {
        if(isMyServiceRunning()) {
            String contact = getString(R.string.app_name);
            String message = getString(R.string.test_diction);

            Intent intent = new Intent(DisMoiToutSmsActivity.this, SmsRecuActivity.class);
            intent.putExtra(SmsRecuActivity.Parameters.DATE.name(), new Date().getTime());
            intent.putExtra(SmsRecuActivity.Parameters.CONTACT_NAME.toString(), contact);
            intent.putExtra(SmsRecuActivity.Parameters.MESSAGE.toString(), message);
            intent.putExtra(SmsRecuActivity.Parameters.CONTACT.name(), new Contact(-1, contact, "0000000000", 0));//FIXME named parameters
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.deactivated, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleStatus() {
        Switch switchActivation = findViewById(R.id.switch_activation);
        boolean myServiceRunning = isMyServiceRunning();
        switchActivation.setChecked(myServiceRunning);
    }

    @Override
	protected void onPause() {
		super.onPause();

        languageChooser().setOnItemSelectedListener(null);
        btnTester().setOnClickListener(null);
        checkBoxReponseVocale().setOnCheckedChangeListener(null);
        checkBoxEmoticones().setOnCheckedChangeListener(null);
        checkBoxUniquementMesContacts().setOnCheckedChangeListener(null);
        checkBoxStepDetector().setOnCheckedChangeListener(null);
        checkBoxHeadSetMode().setOnCheckedChangeListener(null);
        findViewById(R.id.gererContacts).setOnClickListener(null);
        findViewById(R.id.gererContacts).setOnClickListener(null);
	}

	public void initVolumeControl() {
		// Volume

		setVolumeControlStream(STREAM_MUSIC);

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

    private CompoundButton checkBoxUniquementMesContacts() {
        return checkbox(R.id.uniquementContactesTab);
    }

    private CompoundButton checkBoxEmoticones() {
        return checkbox(R.id.translateEmoticonesBtnTab);
    }

    private CompoundButton checkBoxReponseVocale() {
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
                R.layout.custom_spinner_item, list);
        dataAdapter.setDropDownViewResource(R.layout.custom_spinner_item);
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
            findViewById(R.id.stepDetectorCheckBox).setVisibility(View.GONE);
        }
    }

    private void initHeadSetOption() {
        checkBoxHeadSetMode().setChecked(getBoolean(this, HEADSET_MODE));
    }

    public boolean isKitKatWithStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PackageManager pm = getPackageManager();
            return pm.hasSystemFeature (PackageManager.FEATURE_SENSOR_STEP_DETECTOR)
                    && pm.hasSystemFeature (PackageManager.FEATURE_SENSOR_STEP_COUNTER);
        }
        return false;
    }

    private CompoundButton checkBoxStepDetector() {
        return checkbox(R.id.stepDetectorCheckBox);
    }

    private CompoundButton checkBoxHeadSetMode() {
        return checkbox(R.id.headSetModeCheckBox);
    }

    public void openPrivacyPolicy(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://objectifandroid.blogspot.fr/2016/12/politique-de-confidentialite.html"));
        startActivity(intent);
    }

}