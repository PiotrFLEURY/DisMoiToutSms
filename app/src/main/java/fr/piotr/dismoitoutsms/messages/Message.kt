package fr.piotr.dismoitoutsms.messages

import fr.piotr.dismoitoutsms.contacts.Contact
import java.io.Serializable
import java.util.*

data class Message(val smsId: Long = -1,
                   val threadId: Long = -1,
                   var messageType: MessageType,
                   val date: Date,
                   val contact: Contact,
                   var message: String,
                   var read: Boolean = false,
                   var errorCode: Int? = null,
                   val discriminant: Discriminant = Discriminant.SMS,
                   val messageParts: List<MessagePart> = listOf()) : Comparable<Message>, Serializable {

    enum class Discriminant {
        SMS, MMS
    }

    enum class MessageType {
        INBOX, SENT, DRAFT
    }

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
