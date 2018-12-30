package fr.piotr.dismoitoutsms.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log
import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication
import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.contacts.Contacts
import java.lang.IllegalArgumentException

object ContactHelper {

    private const val DISPLAY_NAME_INDEX = 0
    private const val ID_INDEX = 1
    private const val NUMBER_INDEX = 2
    private const val PHOTO_ID_INDEX = 3

    private val ALL_CONTACTS_PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.PHOTO_ID)

    val allContacts: Contacts
        get() = getAllContacts(DisMoiToutSmsApplication.INSTANCE.applicationContext)

    @JvmStatic
    fun getContact(context: Context, phoneNumber: String): Contact {
        val allContacts = getAllContacts(context)
        for (contact in allContacts) {
            if (PhoneNumberUtils.compare(phoneNumber, contact.telephone)) {
                return contact
            }
        }
        return Contact(name = phoneNumber, telephone = phoneNumber)
    }

    fun getContactById(context: Context, contactId: Long): Contact {
        val cr = context.contentResolver
        val contactCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ALL_CONTACTS_PROJECTION, "_id = ?", arrayOf(contactId.toString()), null)

        contactCursor?.use {
            if (contactCursor.moveToFirst()) {
                val name: String? = contactCursor.getString(DISPLAY_NAME_INDEX)
                val id = contactCursor.getInt(ID_INDEX)
                val telephone: String? = contactCursor.getString(NUMBER_INDEX)
                val photoId = contactCursor.getInt(PHOTO_ID_INDEX)

                if (name != null && telephone != null) {
                    Contact(id, name, telephone, photoId)
                }
            }
        }

        throw IllegalArgumentException("No contact found with id $contactId")
    }

    @JvmStatic
    fun getAllContacts(context: Context): Contacts {
        val mesContacts = Contacts()
        val cr = context.contentResolver
        val contactCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ALL_CONTACTS_PROJECTION, null, null, null)
        var name: String?
        var id: Int?
        var telephone: String?
        var photoId: Int?
        if (contactCursor.moveToFirst()) {
            do {
                name = contactCursor.getString(DISPLAY_NAME_INDEX)
                id = contactCursor.getInt(ID_INDEX)
                telephone = contactCursor.getString(NUMBER_INDEX)
                photoId = contactCursor.getInt(PHOTO_ID_INDEX)
                if (name != null && telephone != null) {
                    mesContacts.add(Contact(id, name, telephone, photoId))
                }
            } while (contactCursor.moveToNext())
        }
        contactCursor.close()
        mesContacts.sort()

        return mesContacts
    }

    @JvmStatic
    fun getPhotoContact(context: Context, photoId: Int): Bitmap? {
        val contentResolver = context.contentResolver
        val uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoId.toLong())
        val PHOTO_BITMAP_PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO)

        try {
            contentResolver.query(uri, PHOTO_BITMAP_PROJECTION, null, null, null).use { cursor ->
                var thumbnail: Bitmap? = null
                if (cursor != null && cursor.moveToFirst()) {
                    val thumbnailBytes = cursor.getBlob(0)
                    if (thumbnailBytes != null) {
                        thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0,
                                thumbnailBytes.size)
                    }
                }
                return thumbnail
            }
        } catch (e: Exception) {
            Log.e(ContactHelper::class.java.name, e.message)
            return null
        }

    }
}
