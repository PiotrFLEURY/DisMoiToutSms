package fr.piotr.dismoitoutsms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);

        champRecherche = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);

        mesContacts = getAllContacts(this);
        initContactsSelectiones();
        initChampRecherche();
        initListContacts();

	}

    @Override
    public void onBackPressed() {
        if(findViewById(R.id.contacts_menu_background).getVisibility()==View.VISIBLE){
            toggleMenu(null);
        } else if(findViewById(R.id.action_bar).getVisibility()!=View.VISIBLE) {
            findViewById(R.id.action_bar).setVisibility(View.VISIBLE);
            findViewById(R.id.autoCompleteTextView).setVisibility(View.INVISIBLE);
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
            findViewById(R.id.action_bar).setVisibility(View.VISIBLE);
            findViewById(R.id.autoCompleteTextView).setVisibility(View.INVISIBLE);
        });

        ImageView icSearch = (ImageView)findViewById(R.id.ic_search);
        icSearch.setOnClickListener(v -> {
            findViewById(R.id.action_bar).setVisibility(View.INVISIBLE);
            champRecherche.setVisibility(View.VISIBLE);
            champRecherche.requestFocus();
            champRecherche.performClick();
            InputMethodManager inputMethod = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethod.showSoftInput(champRecherche, InputMethodManager.SHOW_IMPLICIT);
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

    @Override
    protected void onPause() {
        super.onPause();

        final AutoCompleteTextView champRecherche = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        champRecherche.setOnItemClickListener(null);

        ImageView icSearch = (ImageView)findViewById(R.id.ic_search);
        icSearch.setOnClickListener(null);

        if(contacts!=null) {
            contacts.setOnItemClickListener(null);
        }

    }

    public void initListContacts() {
		contactsAdapter = new ContactsAdapter(this, getContactsSelectionnes());
		contacts = (ListView) findViewById(R.id.contacts);
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
	public Object onRetainNonConfigurationInstance() {
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
            toggleMenu(null);
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
            toggleMenu(null);
        };
        MessageBox.confirm(ContactSelectionActivity.this, "",
                getString(R.string.areYouSure), runnable, null);
    }

    public void toggleMenu(View v) {
        final View addAll = findViewById(R.id.btn_add_all);
        final View deleteAll = findViewById(R.id.btn_delete_all);
        final View backGround = findViewById(R.id.contacts_menu_background);
        if(addAll.getVisibility()==View.GONE) {
            Animation showContact = AnimationUtils.loadAnimation(this, R.anim.show_contact_menu);

            addAll.setVisibility(View.VISIBLE);
            deleteAll.setVisibility(View.VISIBLE);
            backGround.setVisibility(View.VISIBLE);

            addAll.startAnimation(showContact);
            deleteAll.startAnimation(showContact);
            backGround.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein));
        } else {
            Animation hideContact = AnimationUtils.loadAnimation(this, R.anim.hide_contact_menu);
            hideContact.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {

                }

                public void onAnimationEnd(Animation animation) {
                    addAll.setVisibility(View.GONE);
                    deleteAll.setVisibility(View.GONE);
                }

                public void onAnimationRepeat(Animation animation) {

                }
            });
            addAll.startAnimation(hideContact);
            deleteAll.startAnimation(hideContact);
            Animation fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout);
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {

                }

                public void onAnimationEnd(Animation animation) {
                    backGround.setVisibility(View.GONE);
                }

                public void onAnimationRepeat(Animation animation) {

                }
            });
            backGround.startAnimation(fadeout);
        }
    }

}
