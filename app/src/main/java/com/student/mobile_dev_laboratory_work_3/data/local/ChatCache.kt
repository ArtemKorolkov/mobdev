package com.student.mobile_dev_laboratory_work_3.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.student.mobile_dev_laboratory_work_3.data.local.entity.ChannelEntity
import com.student.mobile_dev_laboratory_work_3.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** Локальное хранилище чатов и сообщений (SQLite) */
class ChatCache(context: Context) {

    private val dbHelper = ChatDbHelper(context.applicationContext)

    fun observeChannels(): Flow<List<String>> = callbackFlow {
        val listener: () -> Unit = {
            trySend(queryChannels())
        }
        listeners.add(listener)
        listener()
        awaitClose { listeners.remove(listener) }
    }

    fun observeMessages(channel: String): Flow<List<MessageEntity>> = callbackFlow {
        val listener: () -> Unit = {
            trySend(queryMessages(channel))
        }
        listeners.add(listener)
        listener()
        awaitClose { listeners.remove(listener) }
    }

    suspend fun getChannels(): List<String> = withContext(Dispatchers.IO) {
        queryChannels()
    }

    suspend fun getMessages(channel: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        queryMessages(channel)
    }

    suspend fun countSyncedByChannel(channel: String): Int = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM messages WHERE channel_name = ? AND is_pending = 0",
            arrayOf(channel),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    suspend fun upsertChannels(channels: List<ChannelEntity>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            channels.forEach { channel ->
                val values = ContentValues().apply {
                    put("name", channel.name)
                    put("updated_at", channel.updatedAt)
                }
                db.insertWithOnConflict("channels", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyListeners()
    }

    suspend fun upsertMessages(messages: List<MessageEntity>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            messages.forEach { message ->
                db.insertWithOnConflict("messages", null, message.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyListeners()
    }

    suspend fun getOldestServerId(channel: String): String? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            """
            SELECT id FROM messages
            WHERE channel_name = ? AND is_pending = 0
            ORDER BY sort_key ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(channel),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    suspend fun deleteMessageById(id: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("messages", "id = ?", arrayOf(id))
        notifyListeners()
    }

    suspend fun getAllPending(): List<MessageEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM messages WHERE is_pending = 1 ORDER BY created_at ASC",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toMessageEntity())
                }
            }
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("messages", null, null)
            db.delete("channels", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyListeners()
    }

    private fun queryChannels(): List<String> =
        dbHelper.readableDatabase.rawQuery(
            "SELECT name FROM channels ORDER BY name ASC",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }

    private fun queryMessages(channel: String): List<MessageEntity> =
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM messages WHERE channel_name = ? ORDER BY sort_key ASC",
            arrayOf(channel),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toMessageEntity())
                }
            }
        }

    private fun notifyListeners() {
        listeners.toList().forEach { it.invoke() }
    }

    private fun MessageEntity.toContentValues(): ContentValues =
        ContentValues().apply {
            put("id", id)
            put("channel_name", channelName)
            put("from_user", from)
            put("to_user", to)
            put("time", time)
            put("text_body", textBody)
            put("image_link", imageLink)
            put("is_pending", if (isPending) 1 else 0)
            put("sort_key", sortKey)
            put("created_at", createdAt)
        }

    private fun android.database.Cursor.toMessageEntity(): MessageEntity =
        MessageEntity(
            id = getString(getColumnIndexOrThrow("id")),
            channelName = getString(getColumnIndexOrThrow("channel_name")),
            from = getString(getColumnIndexOrThrow("from_user")),
            to = getString(getColumnIndexOrThrow("to_user")),
            time = getString(getColumnIndexOrThrow("time")),
            textBody = getString(getColumnIndexOrThrow("text_body")),
            imageLink = getString(getColumnIndexOrThrow("image_link")),
            isPending = getInt(getColumnIndexOrThrow("is_pending")) == 1,
            sortKey = getLong(getColumnIndexOrThrow("sort_key")),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
        )

    private class ChatDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE channels (
                    name TEXT PRIMARY KEY NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE messages (
                    id TEXT PRIMARY KEY NOT NULL,
                    channel_name TEXT NOT NULL,
                    from_user TEXT NOT NULL,
                    to_user TEXT,
                    time TEXT,
                    text_body TEXT,
                    image_link TEXT,
                    is_pending INTEGER NOT NULL,
                    sort_key INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX index_messages_channel_sort ON messages(channel_name, sort_key)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS messages")
            db.execSQL("DROP TABLE IF EXISTS channels")
            onCreate(db)
        }
    }

    companion object {
        private const val DB_NAME = "faerytea_chat.db"
        private const val DB_VERSION = 1
        private val listeners = mutableSetOf<() -> Unit>()
    }
}
