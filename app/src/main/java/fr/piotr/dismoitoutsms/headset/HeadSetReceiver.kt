package fr.piotr.dismoitoutsms.headset

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import fr.piotr.dismoitoutsms.util.ConfigurationManager

/**
 * Created by piotr on 09/07/2017.
 *
 */

class HeadSetReceiver : AbstractHeadSetReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
            val audioState = intent.getIntExtra("state", -1)
            if (audioState == 1) {
                onHeadSetPluggedIn(context)
            } else if (audioState == 0) {
                onHeadSetPluggedOut(context)
            }
        }
    }

    override fun isHeadsetModeActivated(context: Context): Boolean  {
        return ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.HEADSET_MODE)
    }

}
