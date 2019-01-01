package fr.piotr.dismoitoutsms.messages

import java.io.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

data class Thread(val id: Long, val date: Date, var messageCount: Int, var snippet: String, var recipients: List<String>, var complete: AtomicBoolean = AtomicBoolean(false)): Serializable