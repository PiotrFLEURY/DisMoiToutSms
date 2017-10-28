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
import android.util.Log;

import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication;
import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.contacts.Contacts;

public class ContactHelper {

    private static final int DISPLAY_NAME_INDEX = 0;
    private static final int ID_INDEX = 1;
    private static final int NUMBER_INDEX = 2;
    private static final int PHOTO_ID_INDEX = 3;

    private static final String [] ALL_CONTACTS_PROJECTION = {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_ID
    };

	public static Contact getContact(Context context, String phoneNumber) {
		Contacts allContacts = getAllContacts(context);
		for (Contact contact : allContacts) {
			if (PhoneNumberUtils.compare(phoneNumber, contact.getTelephone())) {
				return contact;
			}
		}
		return null;
	}

	public static  Contacts getAllContacts() {
		return getAllContacts(DisMoiToutSmsApplication.INSTANCE.getApplicationContext());
	}

	private static Contacts getAllContacts(Context context) {
		Contacts mesContacts = new Contacts();
		ContentResolver cr = context.getContentResolver();
		final Cursor contactCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ALL_CONTACTS_PROJECTION,
				null, null, null);
		String name;
		Integer id;
		String telephone;
		Integer photoId;
		if (contactCursor.moveToFirst()) {
			do {
				name = contactCursor.getString(DISPLAY_NAME_INDEX);
				id = contactCursor.getInt(ID_INDEX);
				telephone = contactCursor.getString(NUMBER_INDEX);
				photoId = contactCursor.getInt(PHOTO_ID_INDEX);
				mesContacts.add(new Contact(id, name, telephone, photoId));
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
		Cursor cursor = null;

		try {
			cursor = contentResolver.query(uri, PHOTO_BITMAP_PROJECTION, null, null, null);
			Bitmap thumbnail = null;
			if (cursor!=null && cursor.moveToFirst()) {
				final byte[] thumbnailBytes = cursor.getBlob(0);
				if (thumbnailBytes != null) {
					thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0,
							thumbnailBytes.length);
				}
			}
			return thumbnail;
		} catch (Exception e) {
			Log.e(ContactHelper.class.getName(), e.getMessage());
			return null;
		} finally {
            if(cursor!=null) {
                cursor.close();
            }
		}

	}
}
