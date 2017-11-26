package fr.piotr.dismoitoutsms

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import fr.piotr.dismoitoutsms.contacts.Contact
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
import java.util.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import kotlin.reflect.KFunction0


class DisMoiToutSmsActivity : AbstractActivity() {

    private val isKitKatWithStepCounter: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR)
                        && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
            }
            return false
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        initStepDetectorOption()

        initHeadSetOption()

        drawer_tv_version.text = BuildConfig.VERSION_NAME

        tapTargetFor(switch_activation, "Activer Dis-moi tout", "Activer la diction ici", this::onTapTargetSwitchActivation)

    }

    private fun tapTargetFor(view: Switch?, title: String, text: String, action: KFunction0<Unit>) {
        MaterialTapTargetPrompt.Builder(this@DisMoiToutSmsActivity)
                .setTarget(view)
                .setPrimaryText(title)
                .setSecondaryText(text)
                .setPromptStateChangeListener({ _, state ->
                    if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                        // User has pressed the prompt target
                        action.call()
                    }
                })
                .show()
    }

    fun onTapTargetSwitchActivation(){
        //TODO
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> toggleDrawer()
            R.id.main_menu_people -> openContactSelection(null)
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

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
                if (isChecked) {
                    startActivity(Intent(this@DisMoiToutSmsActivity, ContactSelectionActivity::class.java))
                }
            }
        }

        if (isKitKatWithStepCounter) {
            switch_step_detector.setOnCheckedChangeListener { _, isChecked -> setBoolean(applicationContext, ARRET_STEP_DETECTOR, isChecked) }
        }

        switch_headset_mode.setOnCheckedChangeListener { _, isChecked -> setBoolean(applicationContext, HEADSET_MODE, isChecked) }

        tv_gerer_contacts.setOnClickListener({ this.openContactSelection(it) })

        checkPermissions(AbstractActivity.PERMISSIONS_REQUEST_RESUME,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_PHONE_STATE)

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

        sp_language.onItemSelectedListener = null
        btn_tester.setOnClickListener(null)
        switch_reponse_vocale.setOnCheckedChangeListener(null)
        switch_emoticones.setOnCheckedChangeListener(null)
        switch_uniquement_mes_contacts.setOnCheckedChangeListener(null)
        switch_step_detector.setOnCheckedChangeListener(null)
        switch_headset_mode.setOnCheckedChangeListener(null)
        tv_gerer_contacts.setOnClickListener(null)
    }

    private fun initVolumeControl() {
        // Volume

        volumeControlStream = STREAM_MUSIC

    }

    private fun verifierExistanceServiceSyntheseVocale() {
        try {
            val checkIntent = Intent()
            checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
            startActivityForResult(checkIntent, 0x01)
        } catch (e: ActivityNotFoundException) {
            Log.e(javaClass.simpleName, e.message)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 0x01) {
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

    private fun initStepDetectorOption() {
        if (isKitKatWithStepCounter) {
            switch_step_detector.isChecked = getBoolean(this, ARRET_STEP_DETECTOR)
        } else {
            ll_step_detector.visibility = View.GONE
        }
    }

    private fun initHeadSetOption() {
        switch_headset_mode.isChecked = getBoolean(this, HEADSET_MODE)
    }

    fun openPrivacyPolicy(view: View) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(getString(R.string.privacyPolicyLnk))
        startActivity(intent)
    }

    companion object {

        val TAG = "DisMoiToutSmsActivity"
    }

}