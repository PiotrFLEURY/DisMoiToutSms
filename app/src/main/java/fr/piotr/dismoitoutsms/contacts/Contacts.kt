package fr.piotr.dismoitoutsms.contacts

import java.io.Serializable
import java.util.*

/**
 * Created by piotr_000 on 30/10/2016.
 *
 */

class Contacts : Serializable, Iterable<Contact> {

    internal var contacts: MutableList<Contact> = ArrayList()

    fun add(contact: Contact) {
        if(!containsContact(contact)) {
            this.contacts.add(contact)
        }
    }

    override fun iterator(): Iterator<Contact> {
        return contacts.iterator()
    }

    fun sort() {
        contacts.sort()
    }

    fun asList(): List<Contact> {
        return contacts
    }

    operator fun contains(contact: Contact): Boolean {
        return contacts.contains(contact)
    }

    fun remove(contact: Contact) {
        this.contacts.remove(contact)
    }

    fun clear() {
        this.contacts.clear()
    }

    fun addAll(contacts: Contacts) {
        this.contacts.addAll(contacts.asList())
    }

    private fun containsContact(candidate: Contact): Boolean {
        return this.contacts.any { candidate.name == it.name && candidate.telephone == it.telephone }
    }

    fun isEmpty(): Boolean {
        return contacts.isEmpty()
    }
}
