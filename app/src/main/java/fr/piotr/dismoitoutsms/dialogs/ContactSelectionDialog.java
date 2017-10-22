package fr.piotr.dismoitoutsms.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.contacts.Contacts;
import fr.piotr.dismoitoutsms.contacts.ContactsAdapter;

/**
 * Created by piotr on 19/10/2017.
 *
 */

public class ContactSelectionDialog extends AlertDialog {

    public static final String TAG = "ContactSelectionDialog";

    public static final String EVENT_CONTACT_SELECTED = TAG + ".EVENT_CONTACT_SELECTED";
    public static final String EXTRA_CONTACT_SELECTED = TAG + ".EXTRA_CONTACT_SELECTED";

    private TextView tvTitle;
    private ListView lvContacts;

    private ContactsAdapter adapter;

    public ContactSelectionDialog(Context context) {
        super(context);

        View layout = LayoutInflater.from(context).inflate(R.layout.contact_selection, getListView(), false);

        tvTitle = layout.findViewById(R.id.contact_selection_title);
        lvContacts = layout.findViewById(R.id.contact_selection_list);

        setView(layout);
    }

    public void setContacts(Contacts contacts){
        tvTitle.setText(String.format(Locale.getDefault(), "%d%s", contacts.asList().size(), getContext().getString(R.string.found_contacts)));

        adapter = new ContactsAdapter(getContext(), contacts);
        lvContacts.setAdapter(adapter);
        lvContacts.setOnItemClickListener((adapterView, view, i, l) -> {
            onContactSelected(adapter.getItem(i));
        });
    }

    private void onContactSelected(Contact contact) {
        Intent intent = new Intent(EVENT_CONTACT_SELECTED);
        intent.putExtra(EXTRA_CONTACT_SELECTED, contact);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        dismiss();
    }
}
