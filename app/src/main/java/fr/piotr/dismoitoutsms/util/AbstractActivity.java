package fr.piotr.dismoitoutsms.util;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import fr.piotr.dismoitoutsms.ContactSelectionActivity;
import fr.piotr.dismoitoutsms.DisMoiToutSmsActivity;
import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.reception.ServiceCommunicator;

import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.COMMANDE_VOCALE;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS;
import static fr.piotr.dismoitoutsms.util.ConfigurationManager.setBoolean;

/**
 * Created by piotr_000 on 28/02/2016.
 *
 */
public abstract class AbstractActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_RESUME = 0;
    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    public static final int PERMISSIONS_REQUEST_SMS = 2;
    public static final int PERMISSIONS_RECORD_AUDIO = 3;
    public static final int PERMISSIONS_READ_PHONE_STATE = 4;

    public void go(Class clazz) {
        startActivity(new Intent(this, clazz));
    }

    public boolean isMyServiceRunning() {
        return isMyServiceRunning(ServiceCommunicator.class);
    }

    public boolean isMyServiceRunning(Class<? extends Service> clazz) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (clazz.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void back(View v) {
        onBackPressed();
    }

    public void openContactSelection(View v) {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(Gravity.START);
        if(checkPermissions(PERMISSIONS_REQUEST_READ_CONTACTS, Manifest.permission.READ_CONTACTS)) {
            go(ContactSelectionActivity.class);
        }

    }

    public Button button(int id) {
        return (Button) findViewById(id);
    }

    public CompoundButton checkbox(int id) {
        return (CompoundButton) findViewById(id);
    }

    public Spinner spinner(int id) {
        return (Spinner) findViewById(id);
    }

    public TextView text(int id) {
        return (TextView) findViewById(id);
    }

    protected boolean checkPermissions(int permissionRequestID, String ... permissions){
        for(String permission:permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, permissionRequestID);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted(requestCode);
        } else {
            onPermissionDenied(requestCode);
        }
    }

    protected void onPermissionGranted(int requestCode){
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
//                setBoolean(getApplicationContext(), UNIQUEMENT_CONTACTS, true);
//                checkbox(R.id.uniquementContactesTab).setChecked(true);
//                go(ContactSelectionActivity.class);
                break;
            case PERMISSIONS_REQUEST_SMS:
                break;
            case PERMISSIONS_RECORD_AUDIO:
//                setBoolean(getApplicationContext(), COMMANDE_VOCALE, true);
//                checkbox(R.id.commandeVocaleBtnTab).setChecked(true);
                break;
            case PERMISSIONS_READ_PHONE_STATE:
                break;
        }
    }

    protected void onPermissionDenied(int requestCode) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                setBoolean(getApplicationContext(), UNIQUEMENT_CONTACTS, false);
                checkbox(R.id.uniquementContactesTab).setChecked(false);
                break;
            case PERMISSIONS_REQUEST_SMS:
                break;
            case PERMISSIONS_RECORD_AUDIO:
                setBoolean(getApplicationContext(), COMMANDE_VOCALE, false);
                checkbox(R.id.commandeVocaleBtnTab).setChecked(false);
                break;
            case PERMISSIONS_READ_PHONE_STATE:
                break;
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DisMoiToutSmsActivity.EVENT_DEACTIVATE));
        Toast.makeText(this, R.string.toast_error_permission_denial, Toast.LENGTH_SHORT).show();
    }
}
