package fr.piotr.dismoitoutsms.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.widget.ListView
import android.widget.TextView
import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.contacts.Contacts
import fr.piotr.dismoitoutsms.contacts.ContactsAdapter
import kotlinx.android.synthetic.main.contact_selection.view.*
import java.util.*

/**
 * Created by piotr on 19/10/2017.
 *
 */
class ContactSelectionDialog(context: Context) : AlertDialog(context) {

    companion object {
        private const val TAG = "ContactSelectionDialog"

        const val EVENT_CONTACT_SELECTED = "$TAG.EVENT_CONTACT_SELECTED"
        const val EXTRA_CONTACT_SELECTED = "$TAG.EXTRA_CONTACT_SELECTED"
    }

    private val tvTitle: TextView
    private val lvContacts: ListView

    init {

        val layout = LayoutInflater.from(context).inflate(R.layout.contact_selection, listView, false)

        tvTitle = layout.contact_selection_title
        lvContacts = layout.contact_selection_list

        setView(layout)
    }

    fun setContacts(contacts: Contacts) {
        tvTitle.text = String.format(Locale.getDefault(), "%d%s", contacts.asList().size, context.getString(R.string.found_contacts))

        val adapter = ContactsAdapter(context, contacts)
        lvContacts.adapter = adapter
        lvContacts.setOnItemClickListener { _, _, i, _ -> onContactSelected(adapter.getItem(i)) }
    }

    private fun onContactSelected(contact: Contact?) {
        val intent = Intent(EVENT_CONTACT_SELECTED)
        intent.putExtra(EXTRA_CONTACT_SELECTED, contact)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        dismiss()
    }
}
