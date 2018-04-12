package fr.piotr.dismoitoutsms

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.contacts.Contacts
import fr.piotr.dismoitoutsms.contacts.ContactsAdapter
import fr.piotr.dismoitoutsms.contacts.ContactsAdapter.ContactViewHolder
import fr.piotr.dismoitoutsms.util.AbstractActivity
import fr.piotr.dismoitoutsms.util.ConfigurationManager
import fr.piotr.dismoitoutsms.util.ConfigurationManager.bannirLesContacts
import fr.piotr.dismoitoutsms.util.ContactHelper.getAllContacts
import fr.piotr.dismoitoutsms.util.MessageBox
import kotlinx.android.synthetic.main.contacts.*

/**
 * @author Piotr
 */
class ContactSelectionActivity : AbstractActivity() {

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var dorpDownAdapter: ContactsAdapter

    private val mesContacts: Contacts = getAllContacts()
    private val contactsSelectionnes: Contacts = Contacts()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowHomeEnabled(true)
            supportActionBar.setDisplayHomeAsUpEnabled(true)
            supportActionBar.setTitle(R.string.selectioncontacttitle)
        }
        setContentView(R.layout.contacts)

        initListContactsAdapter()
        initChampRecherche()

        initContactsSelectiones()

    }

    override fun onBackPressed() {
        if (champRecherche.visibility == View.VISIBLE) {
            closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()

        champRecherche.setOnItemClickListener { _, _, _, _ ->
            val text = champRecherche.text.toString()
            for (contact in mesContacts) {
                if (text == contact.toString()) {
                    if (!contactsSelectionnes.contains(contact)) {
                        ajouterContact(contact)
                    }
                    champRecherche.setText("")
                    val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethod.hideSoftInputFromWindow(champRecherche.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
                    break
                }
            }
            closeSearch()
        }

        contacts.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
                val holder = arg1.tag as ContactViewHolder
                confirmerSuppression(holder)
            }

            private fun confirmerSuppression(holder: ContactViewHolder) {
                MessageBox.confirm(context = this@ContactSelectionActivity, title = getString(R.string.suppression),
                        message = getString(R.string.confirmersuppression),
                        ok = Runnable({ supprimerContact(holder.nom.text.toString()) }))
            }

        }

        btn_search.setOnClickListener({toggleSearch()})

    }

    private fun openSearch() {
        contacts.visibility = View.INVISIBLE
        champRecherche.visibility = View.VISIBLE
        champRecherche.requestFocus()
        champRecherche.performClick()
        val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethod.showSoftInput(champRecherche, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        contacts.visibility = View.VISIBLE
        champRecherche.visibility = View.GONE
        val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethod.hideSoftInputFromWindow(champRecherche.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun toggleSearch() {
        if (champRecherche.visibility == View.VISIBLE) {
            closeSearch()
        } else {
            openSearch()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contacts_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.contacts_menu_search -> {
                toggleSearch()
                return true
            }
            R.id.contacts_menu_add_all -> {
                addAll()
                return true
            }
            R.id.contacts_menu_remove_all -> deleteAll()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        champRecherche.onItemClickListener = null

        contacts.onItemClickListener = null
        btn_search.setOnClickListener(null)
    }

    private fun initListContactsAdapter() {
        contactsAdapter = ContactsAdapter(this, contactsSelectionnes)

        contacts.adapter = contactsAdapter
    }

    private fun initContactsSelectiones() {
        val idsContactsBannis = ConfigurationManager.idsContactsBannis
        for (contact in mesContacts) {
            if (!idsContactsBannis.contains(contact.id.toString())) {
                contactsSelectionnes.add(contact)
            }
        }
        contactsAdapter.notifyDataSetChanged()
    }

    private fun ajouterContact(contact: Contact) {
        contactsSelectionnes.add(contact)
        contactsAdapter.notifyDataSetChanged()
        dorpDownAdapter.notifyDataSetChanged()
        enregistrer()
    }

    private fun initChampRecherche() {
        dorpDownAdapter = ContactsAdapter(this, mesContacts)
        champRecherche.setAdapter(dorpDownAdapter)
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return contacts
    }

    fun supprimerContact(contactName: String) {
        for (contact in mesContacts) {
            if (contactName == contact.name) {
                contactsSelectionnes.remove(contact)
                contactsAdapter.notifyDataSetChanged()
                dorpDownAdapter.notifyDataSetChanged()
                break
            }
        }
        enregistrer()
    }

    private fun enregistrer() {

        val contactsABannir = Contacts()
        for (contact in mesContacts) {
            if (!contactsSelectionnes.contains(contact)) {
                contactsABannir.add(contact)
            }
        }

        bannirContacts(contactsABannir)

    }

    private fun bannirContacts(contactsABannir: Contacts) {
        bannirLesContacts(this@ContactSelectionActivity, contactsABannir)
    }

    private fun deleteAll() {
        val runnable = Runnable {
            contactsSelectionnes.clear()
            contactsAdapter.notifyDataSetChanged()
            dorpDownAdapter.notifyDataSetChanged()
            enregistrer()
        }
        MessageBox.confirm(context = this, message = getString(R.string.areYouSure),
                ok = runnable)

    }

    private fun addAll() {
        val runnable = Runnable {
            contactsSelectionnes.clear()
            contactsSelectionnes.addAll(mesContacts)
            contactsSelectionnes.sort()
            contactsAdapter.notifyDataSetChanged()
            dorpDownAdapter.notifyDataSetChanged()
            enregistrer()
        }
        MessageBox.confirm(context = this, message =  getString(R.string.areYouSure), ok = runnable)
    }

}
