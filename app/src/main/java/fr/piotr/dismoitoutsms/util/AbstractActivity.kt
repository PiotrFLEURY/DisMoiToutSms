package fr.piotr.dismoitoutsms.util

import android.Manifest
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import fr.piotr.dismoitoutsms.ContactSelectionActivity
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.COMMANDE_VOCALE
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS

/**
 * Created by piotr_000 on 28/02/2016.
 *
 */
abstract class AbstractActivity : AppCompatActivity() {

    fun openContactSelection() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        if (checkPermissions(PERMISSIONS_REQUEST_READ_CONTACTS, Manifest.permission.READ_CONTACTS)) {
            startActivity(Intent(applicationContext, ContactSelectionActivity::class.java))
        }

    }

    fun checkbox(id: Int): CompoundButton {
        return findViewById<View>(id) as CompoundButton
    }

    fun text(id: Int): TextView {
        return findViewById<View>(id) as TextView
    }

    protected fun checkPermissions(permissionRequestID: Int, vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, permissionRequestID)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted(requestCode)
        } else {
            onPermissionDenied(requestCode)
        }
    }

    private fun onPermissionGranted(requestCode: Int) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
            }
            PERMISSIONS_REQUEST_SMS -> {
            }
            PERMISSIONS_RECORD_AUDIO -> {
            }
            PERMISSIONS_READ_PHONE_STATE -> {
            }
        }
    }

    private fun onPermissionDenied(requestCode: Int) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
                ConfigurationManager.setBoolean(applicationContext, UNIQUEMENT_CONTACTS, false)
                checkbox(R.id.switch_uniquement_mes_contacts).isChecked = false
            }
            PERMISSIONS_REQUEST_SMS -> {
            }
            PERMISSIONS_RECORD_AUDIO -> {
                ConfigurationManager.setBoolean(applicationContext, COMMANDE_VOCALE, false)
                checkbox(R.id.switch_reponse_vocale).isChecked = false
            }
            PERMISSIONS_READ_PHONE_STATE -> {
            }
        }
        Toast.makeText(applicationContext, R.string.toast_error_permission_denial, Toast.LENGTH_SHORT).show()
    }

    companion object {

        const val PERMISSIONS_REQUEST_RESUME = 0
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 1
        const val PERMISSIONS_REQUEST_SMS = 2
        const val PERMISSIONS_RECORD_AUDIO = 3
        const val PERMISSIONS_READ_PHONE_STATE = 4
    }
}
