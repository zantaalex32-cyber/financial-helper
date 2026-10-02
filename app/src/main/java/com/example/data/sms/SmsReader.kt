package com.example.data.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.example.data.model.TransactionEntity
import com.example.data.parser.MpesaSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsReader {

    suspend fun readMpesaSms(context: Context): List<TransactionEntity> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<TransactionEntity>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        // Query messages from MPESA or containing Confirmed. Ksh
        val selection = "${Telephony.Sms.Inbox.ADDRESS} LIKE ? OR ${Telephony.Sms.Inbox.BODY} LIKE ?"
        val selectionArgs = arrayOf("%MPESA%", "%Confirmed%Ksh%")
        val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.let {
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext()) {
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)

                    val parsed = MpesaSmsParser.parse(body, date)
                    if (parsed != null) {
                        transactions.add(parsed)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        transactions
    }
}
