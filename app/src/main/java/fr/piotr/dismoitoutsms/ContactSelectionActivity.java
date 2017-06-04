package fr.piotr.dismoitoutsms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import java.util.List;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.contacts.Contacts;
import fr.piotr.dismoitoutsms.contacts.ContactsAdapter;
import fr.piotr.dismoitoutsms.contacts.ContactsAdapter.ContactViewHolder;
import fr.piotr.dismoitoutsms.util.AbstractActivity;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;
import fr.piotr.dismoitoutsms.util.MessageBox;

import static fr.piotr.dismoitoutsms.util.ConfigurationManager.bannirLesContacts;
import static fr.piotr.dismoitoutsms.util.ContactHelper.getAllContacts;

/**
 * @author Piotr
 * 
 */
public class ContactSelectionActivity extends AbstractActivity {

    AutoCompleteTextView champRecherche;

	ContactsAdapter		contactsAdapter;
	ListView			contacts;

	Contacts mesContacts;

	Contacts       contactsSelectionnes;
	ContactsAdapter		dorpDownAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowHomeEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setTitle(R.string.selectioncontacttitle);
        }
		setContentView(R.layout.contacts);

        champRecherche = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        contacts = (ListView) findViewById(R.id.contacts);

        mesContacts = getAllContacts(this);
        initContactsSelectiones();
        initChampRecherche();
        initListContacts();

	}

    @Override
    public void onBackPressed() {
       if(champRecherche.getVisibility()==View.VISIBLE) {
            closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        champRecherche.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
            String text = champRecherche.getText().toString();
            for (Contact contact : mesContacts) {
                if (text.equals(contact.toString())) {
                    if (!getContactsSelectionnes().contains(contact)) {
                        ajouterContact(contact);
                    }
                    champRecherche.setText("");
                    InputMethodManager inputMethod = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethod.hideSoftInputFromWindow(champRecherche.getWindowToken(),InputMethodManager.HIDE_IMPLICIT_ONLY);
                    break;
                }
            }
            closeSearch();
        });

        if(contacts!=null) {
            contacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    ContactViewHolder holder = (ContactViewHolder) arg1.getTag();
                    confirmerSuppression(holder);
                }

                private void confirmerSuppression(final ContactViewHolder holder) {
                    Runnable runnable = () -> supprimerContact(holder.getNom().getText().toString());
                    MessageBox.confirm(ContactSelectionActivity.this, getString(R.string.suppression),
                            getString(R.string.confirmersuppression), runnable, null);
                }

            });
        }

    }

    private void openSearch() {
        contacts.setVisibility(View.INVISIBLE);
        champRecherche.setVisibility(View.VISIBLE);
        champRecherche.requestFocus();
        champRecherche.performClick();
        InputMethodManager inputMethod = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethod.showSoftInput(champRecherche, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeSearch() {
        contacts.setVisibility(View.VISIBLE);
        champRecherche.setVisibility(View.GONE);
        InputMethodManager inputMethod = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethod.hideSoftInputFromWindow(champRecherche.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void toggleSearch(View v) {
        if(champRecherche.getVisibility()==View.VISIBLE){
            closeSearch();
        } else {
            openSearch();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.contacts_menu_search:
                toggleSearch(null);
                return true;
            case R.id.contacts_menu_add_all:
                addAll(null);
                return true;
            case R.id.contacts_menu_remove_all:
                deleteAll(null);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        final AutoCompleteTextView champRecherche = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        champRecherche.setOnItemClickListener(null);

        if(contacts!=null) {
            contacts.setOnItemClickListener(null);
        }

    }

    public void initListContacts() {
		contactsAdapter = new ContactsAdapter(this, getContactsSelectionnes());

		contacts.setAdapter(contactsAdapter);
	}

	public void initContactsSelectiones() {
        new AsyncTask<Context, Void, List<String>>() {

            @Override
            protected List<String> doInBackground(Context... context) {
                return ConfigurationManager.getIdsContactsBannis(context[0]);
            }

            @Override
            protected void onPostExecute(List<String> idsContactsBannis) {
                for (Contact contact : mesContacts) {
                    if (!idsContactsBannis.contains(String.valueOf(contact.getId()))) {
                        getContactsSelectionnes().add(contact);
                    }
                }
                contactsAdapter.notifyDataSetChanged();
            }
        }.execute(this);
	}

	public void ajouterContact(Contact contact) {
		getContactsSelectionnes().add(contact);
		contactsAdapter.notifyDataSetChanged();
		dorpDownAdapter.notifyDataSetChanged();
		enregistrer();
	}

	public void initChampRecherche() {
		final AutoCompleteTextView champRecherche = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
		dorpDownAdapter = new ContactsAdapter(this, mesContacts);
		champRecherche.setAdapter(dorpDownAdapter);
	}

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return contacts;
    }

    Contacts getContactsSelectionnes() {
		if (contactsSelectionnes == null) {
			contactsSelectionnes = new Contacts();
		}
        contactsSelectionnes.sort();
        return contactsSelectionnes;
	}

	public void supprimerContact(String contactName) {
		for (Contact contact : mesContacts) {
			if (contactName.equals(contact.getName())) {
				getContactsSelectionnes().remove(contact);
				contactsAdapter.notifyDataSetChanged();
				dorpDownAdapter.notifyDataSetChanged();
				break;
			}
		}
		enregistrer();
	}

	public void enregistrer() {

		Contacts contactsABannir = new Contacts();
		for (Contact contact : mesContacts) {
			if (!getContactsSelectionnes().contains(contact)) {
				contactsABannir.add(contact);
			}
		}

        new AsyncTask<Contacts, Void, Void>(){

            @Override
            protected final Void doInBackground(Contacts... contacts) {
                bannirLesContacts(ContactSelectionActivity.this, contacts[0]);
                return null;
            }
        }.execute(contactsABannir);

	}

	public void deleteAll(View v) {
        Runnable runnable = () -> {
            getContactsSelectionnes().clear();
            contactsAdapter.notifyDataSetChanged();
            dorpDownAdapter.notifyDataSetChanged();
            enregistrer();
        };
        MessageBox.confirm(ContactSelectionActivity.this, "",
                getString(R.string.areYouSure), runnable, null);

	}

	public void addAll(View v) {
        Runnable runnable = () -> {
            contactsSelectionnes.clear();
            contactsSelectionnes.addAll(mesContacts);
            contactsSelectionnes.sort();
            contactsAdapter.notifyDataSetChanged();
            dorpDownAdapter.notifyDataSetChanged();
            enregistrer();
        };
        MessageBox.confirm(ContactSelectionActivity.this, "",
                getString(R.string.areYouSure), runnable, null);
    }

}
