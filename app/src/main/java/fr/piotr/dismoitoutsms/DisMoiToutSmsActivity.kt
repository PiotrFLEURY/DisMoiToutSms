package fr.piotr.dismoitoutsms

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.dialogs.BluetoothDevicesSelectionFragment
import fr.piotr.dismoitoutsms.intents.IntentProvider
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator
import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService
import fr.piotr.dismoitoutsms.util.AbstractActivity
import fr.piotr.dismoitoutsms.util.ConfigurationManager
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.*
import fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean
import fr.piotr.dismoitoutsms.util.ConfigurationManager.setBoolean
import kotlinx.android.synthetic.main.drawer_layout_v4.*
import kotlinx.android.synthetic.main.drawer_v4.*
import kotlinx.android.synthetic.main.main_v4.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.util.*


class DisMoiToutSmsActivity : AbstractActivity() {

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {

            when (intent?.action) {
                EVENT_TAP_TARGET_ONLY_CCONTACTS -> tapTargetOnlyContacts()
                EVENT_TAP_TARGET_VOCAL_ANSWER -> tapTargetVocalAnswer()
                EVENT_TAP_TARGET_HEADSET_MODE -> tapTargetHeadsetMode()
                EVENT_TAP_TARGET_BLUETOOTH_HEADSET_MODE -> tapTargetBluetoothHeadsetMode()
                EVENT_TAP_TARGET_PRIVATE_LIFE_MODE -> tapTargetPrivateLifeMode()
                EVENT_END_TUTORIAL -> endTutorial()
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ConfigurationManager.initBluetoothHeadsetMode_migration(this)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_24dp)
        }

        setContentView(R.layout.drawer_layout_v4)

        if (!DisMoiToutSmsApplication.INSTANCE.disMoiToutSmsServiceRunning()) {
            startService(Intent(this, DisMoiToutSmsService::class.java))
        }

        verifierExistanceServiceSyntheseVocale()

        initVolumeControl()

        initLanguageChooser()

        iniContactsControls()

        initEmoticonesControl()

        initBoutonTester()

        initBoutonReponseVocale()

        initHeadSetOption()

        initPrivateLifeOption()

        drawer_tv_version.text = BuildConfig.VERSION_NAME

        if(!ConfigurationManager.getBoolean(this, ConfigurationManager.Configuration.TUTORIAL_DONE)) {
            startTutorial()
        }

    }

    private fun setupBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:" + packageName)
                startActivity(intent)
            }
        }
    }

    private fun tapTargetFor(view: View?, title: String, text: String, action: String) {
        MaterialTapTargetPrompt.Builder(this@DisMoiToutSmsActivity)
                .setTarget(view)
                .setPrimaryText(title)
                .setSecondaryText(text)
                .setPromptStateChangeListener({ _, state ->
                    if (state == MaterialTapTargetPrompt.STATE_DISMISSED
                            || state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                        // User has pressed the prompt target
                        LocalBroadcastManager.getInstance(this@DisMoiToutSmsActivity).sendBroadcastSync(Intent(action))
                    }
                })
                .show()
    }

    fun startTutorial(view: View = tv_drawer_help) {
        drawer_layout.closeDrawer(Gravity.START)
        tapTargetFor(switch_activation, getString(R.string.tutorial_activation_title), getString(R.string.tutorial_activation_text), EVENT_TAP_TARGET_ONLY_CCONTACTS)
    }

    fun tapTargetOnlyContacts(){
        tapTargetFor(switch_uniquement_mes_contacts, getString(R.string.tutorial_contacts_title), getString(R.string.tutorial_contacts_text), EVENT_TAP_TARGET_VOCAL_ANSWER)
    }

    fun tapTargetVocalAnswer() {
        tapTargetFor(switch_reponse_vocale, getString(R.string.tutorial_reponse_vocale_title), getString(R.string.tutorial_reponse_vocale_text), EVENT_TAP_TARGET_PRIVATE_LIFE_MODE)
    }

    fun tapTargetPrivateLifeMode() {
        tapTargetFor(switch_private_life_mode, getString(R.string.tutorial_private_life_mode_title), getString(R.string.tutorial_private_life_mode_text), EVENT_TAP_TARGET_HEADSET_MODE)
    }

    fun tapTargetHeadsetMode() {
        tapTargetFor(switch_headset_mode, getString(R.string.tutorial_headset_mode_title), getString(R.string.tutorial_headset_mode_text), EVENT_TAP_TARGET_BLUETOOTH_HEADSET_MODE)
    }

    fun tapTargetBluetoothHeadsetMode() {
        tapTargetFor(tv_bluetooth_headset_mode, getString(R.string.tutorial_bluetooth_headset_mode_title), getString(R.string.tutorial_bluetooth_headset_mode_text), EVENT_END_TUTORIAL)
    }

    fun endTutorial() {
        ConfigurationManager.setBoolean(this, TUTORIAL_DONE, true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {onHomePressed()}
            //R.id.main_menu_people -> openContactSelection(null)
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onHomePressed() {
        toggleDrawer()
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter()
        intentFilter.addAction(EVENT_TAP_TARGET_ONLY_CCONTACTS)
        intentFilter.addAction(EVENT_TAP_TARGET_VOCAL_ANSWER)
        intentFilter.addAction(EVENT_TAP_TARGET_HEADSET_MODE)
        intentFilter.addAction(EVENT_TAP_TARGET_BLUETOOTH_HEADSET_MODE)
        intentFilter.addAction(EVENT_TAP_TARGET_PRIVATE_LIFE_MODE)
        intentFilter.addAction(EVENT_END_TUTORIAL)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadCastReceiver, intentFilter)

        toggleStatus()

        switch_activation.isChecked = isMyServiceRunning
        switch_activation.setOnClickListener { v ->
            if ((v as Switch).isChecked) {
                onActivate()
            } else {
                onDeactivate()
            }
        }

        sp_language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>, arg1: View?, arg2: Int, arg3: Long) {
                if (arg1 != null) {
                    val locale = (arg1 as TextView).text
                    if (locale != null) {
                        ConfigurationManager.setLangueSelectionnee(applicationContext,
                                locale.toString())
                    }
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
                //
            }

        }

        switch_reponse_vocale.setOnCheckedChangeListener { _, isChecked ->
            if (checkPermissions(AbstractActivity.PERMISSIONS_RECORD_AUDIO, Manifest.permission.RECORD_AUDIO)) {
                setBoolean(applicationContext, COMMANDE_VOCALE, isChecked)
            }
        }

        btn_tester.setOnClickListener { _ -> launchTest() }

        switch_emoticones
                .setOnCheckedChangeListener { _, isChecked ->
                    setBoolean(this@DisMoiToutSmsActivity, EMOTICONES,
                            isChecked)
                }

        switch_uniquement_mes_contacts.setOnCheckedChangeListener { _, isChecked ->
            if (checkPermissions(AbstractActivity.PERMISSIONS_REQUEST_READ_CONTACTS, Manifest.permission.READ_CONTACTS)) {
                setBoolean(applicationContext, UNIQUEMENT_CONTACTS,
                        isChecked)
//                if (isChecked) {
//                    startActivity(Intent(this@DisMoiToutSmsActivity, ContactSelectionActivity::class.java))
//                }
            }
        }

        switch_headset_mode.setOnCheckedChangeListener { _, isChecked -> setHeadsetMode(isChecked) }

        tv_bluetooth_headset_mode.setOnClickListener({ openBluetoothDevicePicker() })

        switch_private_life_mode.setOnCheckedChangeListener { _, isChecked -> setBoolean(applicationContext, PRIVATE_LIFE_MODE, isChecked) }

        tv_gerer_contacts.setOnClickListener({ this.openContactSelection(it) })

        checkPermissions(AbstractActivity.PERMISSIONS_REQUEST_RESUME,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_PHONE_STATE)

        tv_tts_voice_parameter.setOnClickListener({ openTtsVoiceParameter() })

        if(intent.extras?.get("android.intent.extra.REFERRER_NAME")!=null){
            onActivate()
            startActivity(IntentProvider().provideNewSmsIntent(this))
            finish()
        }

    }

    private fun openBluetoothDevicePicker() {
        val bluetoothDevicesSelectionFragment = BluetoothDevicesSelectionFragment()
        bluetoothDevicesSelectionFragment.setOnDismissListener { updateBluetoothHeadsetActivationStatus() }
        bluetoothDevicesSelectionFragment.show(supportFragmentManager, bluetoothDevicesSelectionFragment.tag)
    }

    private fun openTtsVoiceParameter() {
        //startActivityForResult(new Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS))
        //startActivityForResult(Intent("android.settings.VOICE_INPUT_SETTINGS"), ACTIVITY_RESULT_TTS_VOICE_PARAMETER)
        val intent = Intent()
        intent.action = "com.android.settings.TTS_SETTINGS"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

    }

    private fun setHeadsetMode(isChecked: Boolean){
        setBoolean(applicationContext, HEADSET_MODE, isChecked)
        if(isChecked) {
            setupBatteryOptimization()
        }
    }

    private fun setupVolume() {
        // Volume à 100% à l'activation
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val halfVolume = maxVolume / 2
        if (currentVolume < halfVolume) {
            audioManager.setStreamVolume(STREAM_MUSIC, halfVolume, FLAG_PLAY_SOUND or FLAG_SHOW_UI)
        }
    }

    private fun toggleDrawer() {
        if (drawer_layout.isDrawerOpen(Gravity.START)) {
            drawer_layout.closeDrawer(Gravity.START)
        } else {
            drawer_layout.openDrawer(Gravity.START)
        }
    }

    private fun onActivate() {
        if (!isMyServiceRunning) {
            val intent = Intent(this@DisMoiToutSmsActivity, ServiceCommunicator::class.java)
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND)
            setupVolume()
            startService(intent)
            toggleStatus()
        }
    }

    private fun onDeactivate() {
        if (isMyServiceRunning) {
            val intent = Intent(this@DisMoiToutSmsActivity, ServiceCommunicator::class.java)
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND)
            stopService(intent)
            toggleStatus()
        }
    }

    private fun launchTest() {
        if (isMyServiceRunning) {
            val contact = getString(R.string.app_name)
            val message = getString(R.string.test_diction)

            val intent = Intent(this@DisMoiToutSmsActivity, SmsRecuActivity::class.java)
            intent.putExtra(SmsRecuActivity.Parameters.DATE.name, Date().time)
            intent.putExtra(SmsRecuActivity.Parameters.CONTACT_NAME.toString(), contact)
            intent.putExtra(SmsRecuActivity.Parameters.MESSAGE.toString(), message)
            intent.putExtra(SmsRecuActivity.Parameters.CONTACT.name, Contact(name =  contact, telephone = "0000000000"))
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.deactivated, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleStatus() {
        if (isMyServiceRunning) {
            switch_activation.isChecked = true
            tv_status_text.text = getString(R.string.activated)
        } else {
            switch_activation.isChecked = false
            tv_status_text.text = getString(R.string.deactivated)
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadCastReceiver)

        sp_language.onItemSelectedListener = null
        btn_tester.setOnClickListener(null)
        switch_reponse_vocale.setOnCheckedChangeListener(null)
        switch_emoticones.setOnCheckedChangeListener(null)
        switch_uniquement_mes_contacts.setOnCheckedChangeListener(null)
        switch_headset_mode.setOnCheckedChangeListener(null)
        tv_bluetooth_headset_mode.setOnClickListener(null)
        switch_private_life_mode.setOnCheckedChangeListener(null)
        tv_gerer_contacts.setOnClickListener(null)
        tv_tts_voice_parameter.setOnClickListener(null)
    }

    private fun initVolumeControl() {
        // Volume

        volumeControlStream = STREAM_MUSIC

    }

    private fun verifierExistanceServiceSyntheseVocale() {
        try {
            val checkIntent = Intent()
            checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
            startActivityForResult(checkIntent, ACTIVITY_RESULT_TTS_DATA)
        } catch (e: ActivityNotFoundException) {
            Log.e(javaClass.simpleName, e.message)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTIVITY_RESULT_TTS_DATA) {
            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // Echec, aucun moteur n'a été trouvé, on propose à
                // l'utilisateur d'en installer un depuis le Market
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                try {
                    startActivity(installIntent)
                } catch (e: Exception) {
                    Log.e(TAG, e.message)
                }

            }
        }
    }

    private fun initLanguageChooser() {
        val languageChooser = sp_language
        val list = ArrayList<String>()
        val availableLocales = Locale.getAvailableLocales()
        Arrays.sort(availableLocales) { object1, object2 -> object1.displayName.compareTo(object2.displayName) }
        val langueSelectionnee = ConfigurationManager.getLangueSelectionnee(this)
        var selectedPosition = 0
        for ((i, locale) in availableLocales.withIndex()) {
            list.add(locale.displayName)
            if (locale == langueSelectionnee) {
                selectedPosition = i
            }
        }
        val dataAdapter = ArrayAdapter(this,
                R.layout.custom_spinner_item, list)
        dataAdapter.setDropDownViewResource(R.layout.custom_spinner_item)
        languageChooser.adapter = dataAdapter
        languageChooser.setSelection(selectedPosition)

    }

    private fun initBoutonReponseVocale() {
        switch_reponse_vocale.isChecked = getBoolean(this, COMMANDE_VOCALE)
    }

    private fun initBoutonTester() {
        btn_tester.isEnabled = true
    }

    private fun initEmoticonesControl() {
        switch_emoticones.isChecked = getBoolean(this, EMOTICONES)
    }

    private fun iniContactsControls() {
        switch_uniquement_mes_contacts.isChecked = getBoolean(this, UNIQUEMENT_CONTACTS)
    }

    private fun initHeadSetOption() {
        switch_headset_mode.isChecked = getBoolean(this, HEADSET_MODE)
        updateBluetoothHeadsetActivationStatus()
    }

    private fun updateBluetoothHeadsetActivationStatus() {
        tv_bluetooth_headset_mode.text = String.format("%s (%s)", getString(R.string.bluetooth_heaset_mode_option), getBluetoothHeadsetActivationStatus())
    }

    private fun getBluetoothHeadsetActivationStatus(): String {
        if(ConfigurationManager.getBoolean(this, BLUETOOTH_HEADSET_MODE)){
            return getString(R.string.activated)
        }
        return getString(R.string.deactivated)
    }

    private fun initPrivateLifeOption() {
        switch_private_life_mode.isChecked = getBoolean(this, PRIVATE_LIFE_MODE)
    }

    fun openPrivacyPolicy(view: View) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(getString(R.string.privacyPolicyLnk))
        startActivity(intent)
    }

    companion object {

        val TAG = "DisMoiToutSmsActivity"
        val EVENT_TAP_TARGET_ONLY_CCONTACTS = TAG + ".tapTargetOnlyContacts"
        val EVENT_TAP_TARGET_VOCAL_ANSWER = TAG + ".tapTargetVocalAnswer"
        val EVENT_TAP_TARGET_HEADSET_MODE = TAG + ".tapTargetHeadsetMode"
        val EVENT_TAP_TARGET_BLUETOOTH_HEADSET_MODE = TAG + ".tapTargetBluetoothHeadsetMode"
        val EVENT_TAP_TARGET_PRIVATE_LIFE_MODE = TAG + ".tapTargetPrivateLifeMode"
        val EVENT_END_TUTORIAL = TAG + ".endTutorial"

        val ACTIVITY_RESULT_TTS_DATA = 1
    }

}