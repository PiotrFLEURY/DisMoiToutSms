package fr.piotr.dismoitoutsms

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import fr.piotr.dismoitoutsms.messages.Message
import fr.piotr.dismoitoutsms.messages.Thread
import fr.piotr.dismoitoutsms.util.ContactHelper
import java.util.*


object MyMessagesManager {

    const val TAG = "MyMessagesManager"

    private fun fetchSentContentByThreadId(context: Context, threadId: Long): List<Message> {
        // Create Inbox box URI
        val inbox = Telephony.Sms.Sent.CONTENT_URI
        // List required columns
        val cols = arrayOf("_id", Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
        // Get Content Resolver object, which will deal with Content Provider
        val cr = context.contentResolver
        // get Inbox messages from content resolver
        val c = cr.query(inbox, cols, "${Telephony.Sms.Sent.THREAD_ID} = ?", arrayOf(threadId.toString()), null)

        val messages = mutableListOf<Message>()
        c?.let { cursor ->
            Log.d(TAG, "Got ${cursor.count} entries from sent")
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(0)
                    val address = cursor.getString(1)
                    val date = cursor.getLong(2)
                    val body = cursor.getString(3)
                    val read = cursor.getInt(4)

                    Log.d(TAG, "Sent entry id=$id address=$address body=$body")

                    messages.add(Message(id, Date(date), ContactHelper.getContact(context, address), body, read == 1))
                } while (cursor.moveToNext())
            }
        }
        c?.close()

        return messages
    }

    private fun fetchInboxContentByThreadId(context: Context, threadId: Long): List<Message> {
        // Create Inbox box URI
        val inbox = Telephony.Sms.Inbox.CONTENT_URI
        // List required columns
        val cols = arrayOf("_id", Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
        // Get Content Resolver object, which will deal with Content Provider
        val cr = context.contentResolver
        // get Inbox messages from content resolver
        val c = cr.query(inbox, cols, "${Telephony.Sms.Inbox.THREAD_ID} = ?", arrayOf(threadId.toString()), null)

        val messages = mutableListOf<Message>()
        c?.let { cursor ->
            Log.d(TAG, "Got ${cursor.count} entries from inbox")
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(0)
                    val address = cursor.getString(1)
                    val date = cursor.getLong(2)
                    val body = cursor.getString(3)
                    val read = cursor.getInt(4)

                    Log.d(TAG, "Inbox entry id=$id address=$address body=$body")

                    messages.add(Message(id, Date(date), ContactHelper.getContact(context, address), body, read == 1))
                } while (cursor.moveToNext())
            }
        }
        c?.close()

        return messages
    }

    private fun fetchMessageById(context: Context, uri: Uri, smsId: Long): Message {
        // List required columns
        val cols = arrayOf("_id", Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
        // Get Content Resolver object, which will deal with Content Provider
        val cr = context.contentResolver
        // get Inbox messages from content resolver
        val c = cr.query(uri, cols, "_id = ?", arrayOf(smsId.toString()), null)

        c?.use { cursor ->
            Log.d(TAG, "Got ${cursor.count} entries from inbox")
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val address = cursor.getString(1)
                val date = cursor.getLong(2)
                val body = cursor.getString(3)
                val read = cursor.getInt(4)

                Log.d(TAG, "Inbox entry id=$id address=$address body=$body")

                return Message(id, Date(date), ContactHelper.getContact(context, address), body, read == 1)
            }
        }

        throw IllegalArgumentException("no message found with id $smsId")
    }

    fun fetchThreadContent(context: Context, threadId: Long): List<Message> {
        return fetchInboxContentByThreadId(context, threadId)
                .toMutableList()
                .apply {
            addAll(fetchSentContentByThreadId(context, threadId))
        }.sortedBy { it.date }
    }

    fun fetchThreads(context: Context): List<Thread> {
        Log.d(TAG, "fetching Threads")
        val uri = Telephony.Threads.CONTENT_URI
        val cols = arrayOf("_id", Telephony.Threads.DATE, Telephony.Threads.MESSAGE_COUNT, Telephony.Threads.SNIPPET, Telephony.Threads.RECIPIENT_IDS)
        val cursor = context.contentResolver.query(uri, cols, null, null, null)

        val threads = mutableListOf<Thread>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(0)
                    val date = Date(it.getLong(1))
                    val messageCount = it.getInt(2)
                    val snippet = it.getString(3)
                    val recipientIds = it.getString(4).trim().split(" ").map { recipientId -> recipientId.toLong() }
                    threads.add(Thread(id, date, messageCount, snippet, recipientIds))
                } while (it.moveToNext())
            }
        }
        return threads

//        return fetchInboxContent(context)
//                .toMutableList()
//                .apply {
//                    addAll(fetchSentContent(context))
//                }
//                .groupBy { it.contact }
//                .map { group -> Thread(group.key, group.value.sortedBy { it.date }) }
    }

    fun saveToInbox(context: Context, phoneNumber: String, body: String): Message {
        Log.d(TAG, "Saving $phoneNumber $body to inbox")
        val inbox = Telephony.Sms.Inbox.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.ADDRESS, phoneNumber)
        contentValues.put(Telephony.Sms.BODY, body)
        contentValues.put(Telephony.Sms.DATE, System.currentTimeMillis())
        contentValues.put(Telephony.Sms.READ, 0) //"0" for have not read sms and "1" for have read sms
        val newUri = context.contentResolver.insert(inbox, contentValues)!!
        val smsId = ContentUris.parseId(newUri)
        return fetchMessageById(context, Telephony.Sms.Inbox.CONTENT_URI, smsId)
    }

    fun saveToSent(context: Context, phoneNumber: String, body: String): Message {
        Log.d(TAG, "Saving $phoneNumber $body to sent")
        val sent = Telephony.Sms.Sent.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.ADDRESS, phoneNumber)
        contentValues.put(Telephony.Sms.BODY, body)
        contentValues.put(Telephony.Sms.DATE, System.currentTimeMillis())
        contentValues.put(Telephony.Sms.READ, 1) //"0" for have not read sms and "1" for have read sms
        val newUri = context.contentResolver.insert(sent, contentValues)
        val smsId = ContentUris.parseId(newUri)
        return fetchMessageById(context, Telephony.Sms.Sent.CONTENT_URI, smsId)
    }

    fun setSendDate(context: Context, message: Message) {
        val smsId = message.smsId
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.Sent.DATE_SENT, System.currentTimeMillis())
        context.contentResolver.update(Telephony.Sms.Sent.CONTENT_URI, contentValues, "_id = ?", arrayOf(smsId.toString()))
    }

    fun setSendErrorCode(context: Context, message: Message) {
        val smsId = message.smsId
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.Sent.ERROR_CODE, message.errorCode)
        context.contentResolver.update(Telephony.Sms.Sent.CONTENT_URI, contentValues, "_id = ?", arrayOf(smsId.toString()))
    }

    fun markAsRead(context: Context, message: Message) {
        val smsId = message.smsId
        Log.d(TAG, "Marking as read id=$smsId")
        val inbox = Telephony.Sms.Inbox.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.READ, if (message.read) 1 else 0) //"0" for have not read sms and "1" for have read sms
        context.contentResolver.update(inbox, contentValues, "(_id = ", arrayOf(smsId.toString()))
    }

}