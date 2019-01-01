package fr.piotr.dismoitoutsms

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import fr.piotr.dismoitoutsms.util.ContactHelper
import java.util.*
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.BitmapFactory
import fr.piotr.dismoitoutsms.messages.*
import java.io.IOException
import java.text.MessageFormat


object MyMessagesManager {

    const val TAG = "MyMessagesManager"

    private fun fetchDraftContentByThreadId(context: Context, threadId: Long): List<Message> {
        // Create Inbox box URI
        val inbox = Telephony.Sms.Draft.CONTENT_URI
        // List required columns
        val cols = arrayOf("_id", Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
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
//                    val threadId = cursor.getLong(1)
                    val address = cursor.getString(2)
                    val date = cursor.getLong(3)
                    val body = cursor.getString(4)
                    val read = cursor.getInt(5)

                    Log.d(TAG, "Sent entry id=$id address=$address body=$body")

                    messages.add(Message(id, threadId, Message.MessageType.DRAFT, Date(date), ContactHelper.getContactByPhoneNumber(context, address), body, read == 1))
                } while (cursor.moveToNext())
            }
        }
        c?.close()

        return messages
    }

    private fun fetchSentContentByThreadId(context: Context, threadId: Long): List<Message> {
        // Create Inbox box URI
        val inbox = Telephony.Sms.Sent.CONTENT_URI
        // List required columns
        val cols = arrayOf("_id", Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
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
//                    val threadId = cursor.getLong(1)
                    val address = cursor.getString(2)
                    val date = cursor.getLong(3)
                    val body = cursor.getString(4)
                    val read = cursor.getInt(5)

                    Log.d(TAG, "Sent entry id=$id address=$address body=$body")

                    messages.add(Message(id, threadId, Message.MessageType.SENT, Date(date), ContactHelper.getContactByPhoneNumber(context, address), body, read == 1))
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
        val cols = arrayOf("_id", Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
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
//                    val threadId = cursor.getLong(1)
                    val address = cursor.getString(2)
                    val date = cursor.getLong(3)
                    val body = cursor.getString(4)
                    val read = cursor.getInt(5)

                    Log.d(TAG, "Inbox entry id=$id address=$address body=$body")

                    messages.add(Message(id, threadId, Message.MessageType.INBOX, Date(date), ContactHelper.getContactByPhoneNumber(context, address), body, read == 1))
                } while (cursor.moveToNext())
            }
        }
        c?.close()

        return messages
    }

    private fun fetchMessageById(context: Context, uri: Uri, messageType: Message.MessageType, smsId: Long): Message {
        // List required columns
        val cols = arrayOf("_id", Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.READ)
        // Get Content Resolver object, which will deal with Content Provider
        val cr = context.contentResolver
        // get Inbox messages from content resolver
        val c = cr.query(uri, cols, "_id = ?", arrayOf(smsId.toString()), null)

        c?.use { cursor ->
            Log.d(TAG, "Got ${cursor.count} entries from inbox")
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val threadId = cursor.getLong(1)
                val address = cursor.getString(2)
                val date = cursor.getLong(3)
                val body = cursor.getString(4)
                val read = cursor.getInt(5)

                Log.d(TAG, "Inbox entry id=$id address=$address body=$body")

                return Message(id, threadId, messageType, Date(date), ContactHelper.getContactByPhoneNumber(context, address), body, read == 1)
            }
        }

        throw IllegalArgumentException("no message found with id $smsId")
    }

    fun fetchThreadContent(context: Context, threadId: Long): List<Message> {
        return fetchInboxContentByThreadId(context, threadId)
                .toMutableList()
                .apply { addAll(fetchSentContentByThreadId(context, threadId)) }
                .apply { addAll(fetchDraftContentByThreadId(context, threadId)) }
                .apply { addAll(fetchMmsList(context, threadId)) }
                .sortedBy { it.date }
                .toMutableList()
    }

    fun fetchThreads(context: Context, limit: Boolean): List<Thread> {
        Log.d(TAG, "fetching Threads")
        val uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI
        val cols = arrayOf("*")
        val sortOrder = if (limit) {
            "date DESC LIMIT 10"
        } else {
            "date DESC"
        }
        val cursor = context.contentResolver.query(uri, cols, null, null, sortOrder)
        val threads = mutableListOf<Thread>()
        cursor?.use {
            Log.d(TAG, "Got ${it.count} entries from threads")
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndex("thread_id"))
                    Log.d(TAG, "getting thread $id datas")
                    val date = Date(it.getLong(it.getColumnIndex("date")))
                    val messageCount = 0//it.getInt(it.getColumnIndex("message_count"))
                    val recipient = fetchThreadRecipients(context, id)
                    val snippet = it.getString(it.getColumnIndex("body")) ?: ""
                    threads.add(Thread(id, date, messageCount, snippet, recipient))
                } while (it.moveToNext())
            }
        }
        return threads

    }

//    fun fetchSnippetByThreadId(context: Context, threadId: Long): String {
//        Log.d(TAG, "Fetching thread $threadId snippet")
//        var snippet = fetchSnippetByThreadIdIn(context, threadId, Telephony.Sms.CONTENT_URI)
//        if (snippet.isEmpty()) {
//            snippet = fetchSnippetByThreadIdIn(context, threadId, Telephony.Mms.CONTENT_URI)
//        }
//        return snippet
//    }

//    private fun fetchSnippetByThreadIdIn(context: Context, threadId: Long, uri: Uri): String {
//        val cols = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
//        val c = context.contentResolver.query(uri, cols, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "${Telephony.Sms.DATE} DESC")
//        c?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                return cursor.getString(0)
//            }
//        }
//        return ""
//    }

    private fun fetchThreadRecipients(context: Context, threadId: Long): List<String> {
        Log.d(TAG, "Fetching thread $threadId recipients")
//        val myOwnPhoneNumber = getMyOwnPhoneNumber(context)
        val recipients = mutableSetOf<String>()
        val uri = Telephony.Sms.CONTENT_URI
        val c = context.contentResolver.query(uri,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID} = ?",//and ${Telephony.Sms.ADDRESS} <> ?
                arrayOf(threadId.toString()), null
        )
        c?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    recipients.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
        }
        //FIXME MMS recipients
        return recipients.map {
            ContactHelper.getContactByPhoneNumber(context, it).name
        }.toList()
    }

//    fun countMessagesByThreadId(context: Context, threadId: Long): Int {
//        Log.d(TAG, "Counting thread $threadId messages")
//        for (uri in arrayOf(Telephony.Sms.Inbox.CONTENT_URI, Telephony.Sms.Sent.CONTENT_URI)) {
//            val c = context.contentResolver.query(uri,
//                    arrayOf("_id"),
//                    "${Telephony.Sms.THREAD_ID} = ?",
//                    arrayOf(threadId.toString()), null
//            )
//            c?.use { cursor ->
//                return cursor.count
//            }
//        }
//        return 0
//    }

    fun saveToInbox(context: Context, phoneNumber: String, body: String, threadId: Long): Message {
        Log.d(TAG, "Saving $phoneNumber $body to inbox")
        val inbox = Telephony.Sms.Inbox.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.ADDRESS, phoneNumber)
        contentValues.put(Telephony.Sms.BODY, body)
        contentValues.put(Telephony.Sms.DATE, System.currentTimeMillis())
        contentValues.put(Telephony.Sms.READ, 0) //"0" for have not read sms and "1" for have read sms
        contentValues.put(Telephony.Sms.THREAD_ID, threadId)
        val newUri = context.contentResolver.insert(inbox, contentValues)!!
        val smsId = ContentUris.parseId(newUri)
        return fetchMessageById(context, Telephony.Sms.Inbox.CONTENT_URI, Message.MessageType.INBOX, smsId)
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun getMyOwnPhoneNumber(context: Context): String {
        val tMgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tMgr.line1Number
    }

    private fun saveToSent(context: Context, message: Message): Message {
        val body = message.message
        val phoneNumber = getMyOwnPhoneNumber(context)
        Log.d(TAG, "Saving $phoneNumber $body to sent")
        val sent = Telephony.Sms.Sent.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.ADDRESS, phoneNumber)
        contentValues.put(Telephony.Sms.BODY, body)
        contentValues.put(Telephony.Sms.DATE, System.currentTimeMillis())
        contentValues.put(Telephony.Sms.READ, 1) //"0" for have not read sms and "1" for have read sms
        contentValues.put(Telephony.Sms.THREAD_ID, message.threadId)
        contentValues.put(Telephony.Sms.Sent.DATE_SENT, System.currentTimeMillis())
        val newUri = context.contentResolver.insert(sent, contentValues)
        val smsId = ContentUris.parseId(newUri)
        return fetchMessageById(context, Telephony.Sms.Sent.CONTENT_URI, Message.MessageType.SENT, smsId)
    }

    fun saveToDraft(context: Context, body: String, threadId: Long): Message {
        val phoneNumber = getMyOwnPhoneNumber(context)
        Log.d(TAG, "Saving $phoneNumber $body to draft")
        val sent = Telephony.Sms.Draft.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.ADDRESS, phoneNumber)
        contentValues.put(Telephony.Sms.BODY, body)
        contentValues.put(Telephony.Sms.DATE, System.currentTimeMillis())
        contentValues.put(Telephony.Sms.READ, 1) //"0" for have not read sms and "1" for have read sms
        contentValues.put(Telephony.Sms.THREAD_ID, threadId)
        val newUri = context.contentResolver.insert(sent, contentValues)
        val smsId = ContentUris.parseId(newUri)
        return fetchMessageById(context, Telephony.Sms.Draft.CONTENT_URI, Message.MessageType.DRAFT, smsId)
    }

    fun transformDraftToSent(context: Context, message: Message): Message {
        val smsId = message.smsId
        context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "_id = ?", arrayOf(smsId.toString()))
        return saveToSent(context, message)
    }

    fun setSendErrorCode(context: Context, message: Message) {
        val smsId = message.smsId
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.Sent.ERROR_CODE, message.errorCode)
        context.contentResolver.update(Telephony.Sms.Draft.CONTENT_URI, contentValues, "_id = ?", arrayOf(smsId.toString()))
    }

    fun markAsRead(context: Context, messages: List<Message>, read: Boolean = true) {
        val smsIds = messages.map { it.smsId }
        Log.d(TAG, "Marking as read ids=$smsIds")
        val inbox = Telephony.Sms.Inbox.CONTENT_URI
        val contentValues = ContentValues()
        contentValues.put(Telephony.Sms.READ, if (read) 1 else 0) //"0" for have not read sms and "1" for have read sms
        val inPlaceholder = (1..smsIds.size).joinToString(separator = ", ") { "?" }
        context.contentResolver.update(inbox, contentValues, "_id in ($inPlaceholder)", smsIds.map { it.toString() }.toTypedArray())

        messages.forEach { it.read = read }
    }

//-----------------------------MMS PART-------------------------------------------------------//
// see https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android/12865692

    private fun fetchMmsList(context: Context, threadId: Long): List<Message> {
        val uri = Telephony.Mms.CONTENT_URI
//        val cols = arrayOf(Telephony.Mms.MESSAGE_ID)
        val c = context.contentResolver.query(uri, null,
                "${Telephony.Mms.THREAD_ID} = ?", arrayOf(threadId.toString()), null)
        val messages = mutableListOf<Message>()
        c?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val mmsId = cursor.getLong(cursor.getColumnIndex("_id"))
//                    val threadId = cursor.getLong(cursor.getColumnIndex(Telephony.Mms.THREAD_ID))
                    val messageBox = cursor.getString(cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX))
                    val date = cursor.getLong(cursor.getColumnIndex(Telephony.Mms.DATE)) * 1000
                    val address = getMmsAddress(context, mmsId)
                    val contact = ContactHelper.getContactByPhoneNumber(context, address)
                    val parts = getMmsContent(context, mmsId)
                    messages.add(Message(mmsId, threadId, messageBoxToMessageType(messageBox.toInt()),
                            Date(date), contact, "", true, null, Message.Discriminant.MMS, parts))
                } while (cursor.moveToNext())
            }
        }
        return messages
    }

    private fun messageBoxToMessageType(messageBox: Int): Message.MessageType {
        return when (messageBox) {
            Telephony.BaseMmsColumns.MESSAGE_BOX_INBOX -> Message.MessageType.INBOX
            Telephony.BaseMmsColumns.MESSAGE_BOX_SENT -> Message.MessageType.SENT
            Telephony.BaseMmsColumns.MESSAGE_BOX_DRAFTS -> Message.MessageType.DRAFT
            else -> TODO("not implemented MESSAGE_BOX $messageBox")
        }
    }

    private fun getMmsAddress(context: Context, mmsId: Long): String {
        val uri = MessageFormat.format("content://mms/{0}/addr", mmsId)
        val c = context.contentResolver.query(Uri.parse(uri), null,
                "${Telephony.Mms.Addr.MSG_ID} = ?", arrayOf(mmsId.toString()), null)
        c?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS))
            }
        }
        throw java.lang.IllegalArgumentException("unable to find address for mms $mmsId")
    }

    private fun getMmsContent(context: Context, mmsId: Long): List<MessagePart> {
        val uri = Uri.parse("content://mms/part")
        val c = context.contentResolver.query(uri, null,
                "${Telephony.Mms.Part.MSG_ID} = ?", arrayOf(mmsId.toString()), null)

        val parts = mutableListOf<MessagePart>()
        c?.use { cursor ->

            if (cursor.moveToFirst()) {
                do {
                    val partId = cursor.getString(cursor.getColumnIndex("_id"))
                    val type = cursor.getString(cursor.getColumnIndex("ct"))
                    if ("text/plain" == type) {
                        val data = cursor.getString(cursor.getColumnIndex("_data"))
                        val body = if (data != null) {
                            // implementation of this method below
                            getMmsText(context, partId)
                        } else {
                            cursor.getString(cursor.getColumnIndex("text"))
                        }
                        parts.add(TextPart(body))
                    } else if ("image/jpeg" == type || "image/bmp" == type ||
                            "image/gif" == type || "image/jpg" == type ||
                            "image/png" == type) {
                        val bitmap = getMmsImage(context, partId)
                        parts.add(ImagePart(bitmap))
                    }
                } while (cursor.moveToNext())
            }
        }
        return parts
    }

    private fun getMmsText(context: Context, id: String): String {
        val partURI = Uri.parse("content://mms/part/$id")
        val sb = StringBuilder()
        try {
            val inputStream = context.contentResolver.openInputStream(partURI)
            inputStream?.use {

                val isr = InputStreamReader(inputStream, "UTF-8")
                val reader = BufferedReader(isr)
                var temp = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }

            }
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        }
        return sb.toString()
    }

    private fun getMmsImage(context: Context, _id: String): Bitmap {
        val partURI = Uri.parse("content://mms/part/$_id")
        try {
            val inputStream = context.contentResolver.openInputStream(partURI)
            inputStream?.use {

                return BitmapFactory.decodeStream(inputStream)

            }
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        }
        throw java.lang.IllegalArgumentException("unable to read mms image with id $_id")
    }
}