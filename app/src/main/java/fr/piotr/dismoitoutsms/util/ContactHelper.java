package fr.piotr.dismoitoutsms.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.contacts.Contacts;

public class ContactHelper {

	public static Contact getContact(Context context, String phoneNumber) {
		Contacts allContacts = getAllContacts(context);
		for (Contact contact : allContacts) {
			if (PhoneNumberUtils.compare(phoneNumber, contact.getTelephone())) {
				return contact;
			}
		}
		return null;
	}

	public static Contacts getAllContacts(Context context) {
		Contacts mesContacts = new Contacts();
		ContentResolver cr = context.getContentResolver();
		final Cursor contactCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
				null, null, null);
		int nameIdx = contactCursor
				.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
		int idIdx = contactCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID);
		int phoneNumberIdx = contactCursor
				.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
		int photoIdx = contactCursor
				.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_ID);
		String name;
		Integer id;
		String telephone;
		Integer photoId;
		if (contactCursor.moveToFirst()) {
			do {
				// if (Integer.parseInt(contact.getString(contact
				// .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)))
				// > 0) {
				name = contactCursor.getString(nameIdx);
				id = contactCursor.getInt(idIdx);
				telephone = contactCursor.getString(phoneNumberIdx);
				photoId = contactCursor.getInt(photoIdx);
				mesContacts.add(new Contact(id, name, telephone, photoId));
				// }
			} while (contactCursor.moveToNext());
		}
        contactCursor.close();
		mesContacts.sort();

		return mesContacts;
	}

	public static Bitmap getPhotoContact(Context context, final int photoId) {
		ContentResolver contentResolver = context.getContentResolver();
		final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoId);
		String[] PHOTO_BITMAP_PROJECTION = new String[] { ContactsContract.CommonDataKinds.Photo.PHOTO };
		final Cursor cursor = contentResolver.query(uri, PHOTO_BITMAP_PROJECTION, null, null, null);

		try {
			Bitmap thumbnail = null;
			if (cursor.moveToFirst()) {
				final byte[] thumbnailBytes = cursor.getBlob(0);
				if (thumbnailBytes != null) {
					thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0,
							thumbnailBytes.length);
				}
			}
			return thumbnail;
		} finally {
			cursor.close();
		}

	}
}
