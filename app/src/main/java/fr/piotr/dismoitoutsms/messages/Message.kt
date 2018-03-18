package fr.piotr.dismoitoutsms.messages

import fr.piotr.dismoitoutsms.contacts.Contact
import java.util.*

data class Message(val date: Date, val contact: Contact, var message: String) : Comparable<Message> {

    fun contactIs(contact: Contact): Boolean {
        return this.contact == contact
    }

    fun append(message: String) {
        this.message += " $message"
    }

    override fun compareTo(other: Message): Int {
        return this.date.compareTo(other.date)
    }
}
