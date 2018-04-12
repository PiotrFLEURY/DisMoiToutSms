package fr.piotr.dismoitoutsms.util

import android.content.Context
import android.text.TextUtils
import android.util.Log
import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication
import fr.piotr.dismoitoutsms.contacts.Contacts
import java.io.*
import java.util.*

/**
 * @author Piotr
 */
object ConfigurationManager {

    private val configuration = HashMap<String, String>()
    private const val TAG = "ConfigurationManager"
    private const val FICHIER = "DisMoisToutSms.properties"
    private const val SEPARATOR = ";"

    val idsContactsBannis: List<String>
        get() {
            val context = DisMoiToutSmsApplication.INSTANCE.applicationContext
            var valeur: String? = getValue(context, Configuration.CONTACTS_BANNIS)
            if (valeur == null) {
                valeur = ""
            }
            return ArrayList(Arrays.asList(*valeur.split(SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        }

    enum class Configuration {
        //@formatter:off
        TUTORIAL_DONE,
        EMOTICONES,
        COMMANDE_VOCALE,
        UNIQUEMENT_CONTACTS,
        CONTACTS_BANNIS,
        LANGUE_DICTION,
        HEADSET_MODE,
        BLUETOOTH_HEADSET_MODE,
        PRIVATE_LIFE_MODE,
        BLUETOOTH_DEVICES_BANNED
        //@formatter:on
    }

    private fun setValue(context: Context, conf: Configuration, valeur: String) {
        charger(context)
        configuration[conf.toString()] = valeur
        sauvegarder(context)
    }

    @JvmStatic
    fun setBoolean(context: Context, conf: Configuration, valeur: Boolean) {
        setValue(context, conf, valeur.toString())
    }

    private fun sauvegarder(context: Context) {

        var ous: FileOutputStream? = null
        var osw: OutputStreamWriter? = null
        try {
            // Crée un flux de sortie vers un fichier local.
            ous = context.openFileOutput(FICHIER, Context.MODE_PRIVATE)
            osw = OutputStreamWriter(ous!!)
            for ((key, value) in configuration) {
                osw.write("$key=$value\n")
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } finally {
            osw?.close()
            ous?.close()
        }

    }

    private fun charger(context: Context) {
        var ins: FileInputStream? = null
        var isr: InputStreamReader? = null
        var br: BufferedReader? = null
        try {
            // Crée un flux d’entrée depuis un fichier local.
            ins = context.openFileInput(FICHIER)
            isr = InputStreamReader(ins!!)
            br = BufferedReader(isr)
            var line: String? = br.readLine()
            while (line != null) {
                val split = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val cle = split[0]
                var valeur = ""
                if (split.size == 2) {
                    valeur = split[1]
                }
                configuration[cle] = valeur
                line = br.readLine()
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } finally {
            try {
                br?.close()
                isr?.close()
                ins?.close()
            } catch (e: IOException) {
                Log.e(TAG, e.message, e)
            }

        }
    }

    private fun getValue(context: Context, conf: Configuration): String {
        charger(context)
        var valeur: String? = configuration[conf.toString()]
        if (valeur == null) {
            valeur = ""
        }
        return valeur
    }

    @JvmStatic
    fun getBoolean(context: Context, config: Configuration): Boolean {
        return java.lang.Boolean.TRUE.toString() == getValue(context, config)
    }

    fun bannirLesContacts(context: Context, contactsABannir: Contacts) {
        val valeur = StringBuilder()
        for ((id) in contactsABannir) {
            valeur.append(id)
            valeur.append(SEPARATOR)
        }
        setValue(context, Configuration.CONTACTS_BANNIS, valeur.toString())
    }

    @JvmStatic
    fun leContactEstBannis(context: Context, id: Int?): Boolean {
        val valeur = getValue(context, Configuration.CONTACTS_BANNIS)
        val bannis = Arrays.asList<String>(*valeur.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
        for (cle in bannis) {
            if (cle == id!!.toString()) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getLangueSelectionnee(context: Context): Locale? {
        val string = getValue(context, Configuration.LANGUE_DICTION)
        if (string.isEmpty()) {
            return Locale.getDefault()
        }
        val availableLocales = Locale.getAvailableLocales()
        for (locale in availableLocales) {
            if (locale.displayName == string) {
                return locale
            }
        }
        return null
    }

    fun setLangueSelectionnee(context: Context, string: String) {
        val availableLocales = Locale.getAvailableLocales()
        for (locale in availableLocales) {
            if (locale.displayName == string) {
                setValue(context, Configuration.LANGUE_DICTION, string)
            }
        }
    }

    fun initBluetoothHeadsetModeFormigration(context: Context) {
        val value = getValue(context, Configuration.BLUETOOTH_HEADSET_MODE)
        if (TextUtils.isEmpty(value)) {
            setBoolean(context, Configuration.BLUETOOTH_HEADSET_MODE, getBoolean(context, Configuration.HEADSET_MODE))
        }
    }

    private fun getBluetoothBanned(context: Context): MutableList<String> {
        var valeur: String? = getValue(context, Configuration.BLUETOOTH_DEVICES_BANNED)
        if (valeur == null) {
            valeur = ""
        }
        return ArrayList(Arrays.asList(*valeur.split(SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
    }

    @JvmStatic
    fun isBluetoothBanned(context: Context, address: String): Boolean {
        return getBluetoothBanned(context).contains(address)
    }

    private fun banBluetoothDevice(context: Context, addressToBan: String) {
        val bluetoothBanned = getBluetoothBanned(context)
        if (!bluetoothBanned.contains(addressToBan)) {
            bluetoothBanned.add(addressToBan)
        }
        val valeur = StringBuilder()
        for (address in bluetoothBanned) {
            valeur.append(address)
            valeur.append(SEPARATOR)
        }
        setValue(context, Configuration.BLUETOOTH_DEVICES_BANNED, valeur.toString())
    }

    private fun grantBluetoothDevice(context: Context, addressToBan: String) {
        val bluetoothBanned = getBluetoothBanned(context)
        if (bluetoothBanned.contains(addressToBan)) {
            bluetoothBanned.remove(addressToBan)
        }
        val valeur = StringBuilder()
        for (address in bluetoothBanned) {
            valeur.append(address)
            valeur.append(SEPARATOR)
        }
        setValue(context, Configuration.BLUETOOTH_DEVICES_BANNED, valeur.toString())
    }

    @JvmStatic
    fun toggleBluetoothDevice(context: Context, address: String, checked: Boolean) {
        if (checked) {
            grantBluetoothDevice(context, address)
        } else {
            banBluetoothDevice(context, address)
        }
    }

}
