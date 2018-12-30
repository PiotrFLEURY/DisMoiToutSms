package fr.piotr.dismoitoutsms

import android.app.Service
import android.content.Intent
import android.os.IBinder

import androidx.annotation.Nullable

class HeadlessSmsSendService : Service() {

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
