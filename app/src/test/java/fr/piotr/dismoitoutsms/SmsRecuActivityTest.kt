package fr.piotr.dismoitoutsms

import fr.piotr.dismoitoutsms.contacts.Contact
import fr.piotr.dismoitoutsms.contacts.Contacts
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Created by piotr on 17/03/2018.
 *
 */
class SmsRecuActivityTest {

    @Test
    fun getCorrespondingContacts() {
        assertFalse(SmsRecuActivity().getCorrespondingContacts(somesContacts(), "Maman").isEmpty())
    }

    private fun somesContacts(): Contacts {
        val contacts = Contacts()
        contacts.add(Contact(id = 1, name = "maman", telephone = "0100000000"))
        return contacts
    }

}