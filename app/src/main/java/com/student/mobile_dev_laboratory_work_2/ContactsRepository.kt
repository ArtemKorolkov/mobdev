package com.student.mobile_dev_laboratory_work_2

import Contact
import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

private data class ContactBuilder(
    var name: String? = null,
    val phones: MutableSet<String> = linkedSetOf(),
    val emails: MutableSet<String> = linkedSetOf()
)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    val contacts = linkedMapOf<Long, ContactBuilder>()

    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (cursor.moveToNext()) {
            val id = cursor.getLongOrNull(idIndex) ?: continue
            val name = cursor.getStringOrNull(nameIndex)
            val phone = cursor.getStringOrNull(phoneIndex)

            val builder = contacts.getOrPut(id) { ContactBuilder() }
            if (!name.isNullOrBlank()) builder.name = name
            if (!phone.isNullOrBlank()) builder.phones.add(phone)
        }
    }

    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        null, null, null, null
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

        while (cursor.moveToNext()) {
            val id = cursor.getLongOrNull(idIndex) ?: continue
            val email = cursor.getStringOrNull(emailIndex)

            val builder = contacts.getOrPut(id) { ContactBuilder() }
            if (!email.isNullOrBlank()) builder.emails.add(email)
        }
    }

    return contacts.values.map {
        val result = contacts.values.map {
            Contact(
                name = it.name ?: "Без имени",
                phones = it.phones.toList(),
                emails = it.emails.toList()
            )
        }

        return if (result.isEmpty()) {
            generateFakeContacts()
        } else {
            result
        }
    }
}


fun generateFakeContacts(): List<Contact> {
    return listOf(
        Contact(
            name = "Иван Иванов",
            phones = listOf("+7 999 123-45-67"),
            emails = listOf("ivan@mail.com")
        ),
        Contact(
            name = "Петр Петров",
            phones = listOf("+7 999 765-43-21"),
            emails = listOf("petr@mail.com")
        ),
        Contact(
            name = "Анна Смирнова",
            phones = listOf("+7 999 111-22-33"),
            emails = listOf("anna@mail.com")
        ),
        Contact(
            name = "Test User",
            phones = listOf("+1 555 000 111"),
            emails = emptyList()
        )
    )
}