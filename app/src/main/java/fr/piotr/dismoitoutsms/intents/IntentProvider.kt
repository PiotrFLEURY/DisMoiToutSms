package fr.piotr.dismoitoutsms.intents

import android.content.Context
import android.content.Intent
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.SmsRecuActivity
import fr.piotr.dismoitoutsms.contacts.Contact
import java.util.*

/**
 * Created by piotr on 24/12/2017.
 *
 */
class IntentProvider {

    companion object {
        val INTENT_TARGET = "INTENT_TARGET"
        val TARGET_NEW_MESSAGE = "TARGET_NEW_MESSAGE"
    }

    fun provideNewSmsIntent(context: Context): Intent {
        val intent = Intent(context, SmsRecuActivity::class.java)
        intent.putExtra(INTENT_TARGET, TARGET_NEW_MESSAGE)
        intent.putExtra(SmsRecuActivity.Parameters.DATE.name, Date().time)

        val contact = context.getString(R.string.app_name)
        intent.putExtra(SmsRecuActivity.Parameters.CONTACT_NAME.toString(), contact)
        intent.putExtra(SmsRecuActivity.Parameters.CONTACT.name, Contact(id = -1, name =  contact, telephone =  "0000000000", photoId =  0))
        return intent
    }

}