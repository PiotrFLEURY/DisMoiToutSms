package fr.piotr.dismoitoutsms.messages

import fr.piotr.dismoitoutsms.contacts.Contact
import java.io.Serializable
import java.util.*

data class Thread(val id: Long, val date: Date, val messageCount: Int, val snippet: String, val recipientIds: List<Long>): Serializable