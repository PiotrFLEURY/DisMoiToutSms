package fr.piotr.dismoitoutsms.contacts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.util.ContactHelper;

public class ContactsAdapter extends ArrayAdapter<Contact> {

	private Context context;

	public ContactsAdapter(Context context, Contacts contacts) {
		super(context, R.layout.contact, contacts.asList());
		this.context = context;
	}

	public static class ContactViewHolder {
		private ImageView avatar;
		private ImageView	photo;
		private TextView	nom;

		public ContactViewHolder() {
			//
		}

		public ContactViewHolder(ImageView avatar, ImageView	photo, TextView nom) {
			this.avatar = avatar;
			this.photo = photo;
			this.nom = nom;
		}

		public ImageView getAvatar() {
			return avatar;
		}

		public ImageView getPhoto() {
			return photo;
		}

		/**
		 * @return the nom
		 */
		public TextView getNom() {
			return nom;
		}

	}

	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		final Contact contact = getItem(position);

		ImageView avatar;
		ImageView photo;
		final TextView textViewNom;
		if (convertView == null) {

			convertView = LayoutInflater.from(context).inflate(R.layout.contact, parent, false);
			avatar = convertView.findViewById(R.id.avatar);
			photo = convertView.findViewById(R.id.photoContact);
			textViewNom = convertView.findViewById(R.id.nom);

			convertView.setTag(new ContactViewHolder(avatar, photo, textViewNom));

		} else {
			ContactViewHolder holder = (ContactViewHolder) convertView.getTag();
			avatar = holder.getAvatar();
			photo = holder.getPhoto();
			textViewNom = holder.getNom();
		}

		if (contact.hasAPhoto()) {
			photo.setVisibility(View.VISIBLE);
			photo.setImageBitmap(ContactHelper.getPhotoContact(context, contact.getPhotoId()));
			avatar.setVisibility(View.GONE);
		} else {
			photo.setVisibility(View.GONE);
			avatar.setVisibility(View.VISIBLE);
		}
		textViewNom.setText(contact.getName());

		return convertView;
	}

}
