package com.student.mobile_dev_laboratory_work_3.data.repository

import com.student.mobile_dev_laboratory_work_3.data.api.FaeryTeaApi
import com.student.mobile_dev_laboratory_work_3.data.api.FaeryTeaHttpException
import com.student.mobile_dev_laboratory_work_3.data.local.ChatCache
import com.student.mobile_dev_laboratory_work_3.data.local.entity.ChannelEntity
import com.student.mobile_dev_laboratory_work_3.data.local.entity.MessageEntity
import com.student.mobile_dev_laboratory_work_3.data.mapper.toChatMessage
import com.student.mobile_dev_laboratory_work_3.data.mapper.toEntities
import com.student.mobile_dev_laboratory_work_3.data.mapper.toEntity
import com.student.mobile_dev_laboratory_work_3.data.model.ChatMessage
import com.student.mobile_dev_laboratory_work_3.data.model.LoginRequest
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.OutgoingContentDto
import com.student.mobile_dev_laboratory_work_3.data.model.OutgoingMessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.TextPayloadDto
import com.student.mobile_dev_laboratory_work_3.data.network.NetworkMonitor
import com.student.mobile_dev_laboratory_work_3.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/** Результат сетевой операции */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val isUnauthorized: Boolean = false) : Result<Nothing>()
}

data class ChannelsData(
    val channels: List<String>,
    val fromCache: Boolean,
)

data class MessagesData(
    val canLoadMore: Boolean,
    val fromCache: Boolean,
)

data class SendMessageData(
    val queued: Boolean,
)

/**
 * Репозиторий: сеть + локальный кэш Room + очередь исходящих сообщений.
 */
class ChatRepository(
    private val api: FaeryTeaApi,
    private val sessionManager: SessionManager,
    private val cache: ChatCache,
    private val networkMonitor: NetworkMonitor,
) {

    fun observeNetworkOnline(): Flow<Boolean> = networkMonitor.isOnline

    fun observeMessages(channel: String): Flow<List<ChatMessage>> =
        cache.observeMessages(channel).map { entities ->
            entities.map { it.toChatMessage() }
        }

    fun observeChannels(): Flow<List<String>> = cache.observeChannels()

    suspend fun login(name: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(name, password))
            response.use {
                when {
                    it.isSuccessful -> {
                        val token = it.body?.string()?.trim().orEmpty()
                        if (token.isEmpty()) {
                            Result.Error(MSG_EMPTY_TOKEN)
                        } else {
                            sessionManager.saveCredentials(name, password, token)
                            Result.Success(Unit)
                        }
                    }
                    it.code == HTTP_UNAUTHORIZED ->
                        Result.Error(MSG_WRONG_CREDENTIALS, isUnauthorized = false)
                    else -> Result.Error(MSG_LOGIN_FAILED.format(it.code))
                }
            }
        } catch (_: FaeryTeaHttpException) {
            Result.Error(MSG_NETWORK_ERROR)
        } catch (_: IOException) {
            Result.Error(MSG_NETWORK_ERROR)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (networkMonitor.isCurrentlyOnline()) {
                api.logout()
            }
        } catch (_: Exception) {
            // офлайн-выход всё равно очищаем локальные данные
        } finally {
            sessionManager.clearToken()
            clearCache()
        }
        Result.Success(Unit)
    }

    suspend fun getChannels(): Result<ChannelsData> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyOnline()) {
            return@withContext cachedChannelsOrError()
        }
        try {
            val remote = api.getChannels()
            cache.upsertChannels(remote.map { ChannelEntity(name = it) })
            Result.Success(ChannelsData(channels = remote, fromCache = false))
        } catch (e: FaeryTeaHttpException) {
            if (e.code == HTTP_UNAUTHORIZED) {
                mapHttpError(e)
            } else {
                cachedChannelsOrError()
            }
        } catch (_: IOException) {
            cachedChannelsOrError()
        }
    }

    suspend fun refreshMessages(channel: String): Result<MessagesData> =
        fetchAndCacheMessages(channel, lastKnownId = "0", reverse = false)

    suspend fun loadOlderMessages(channel: String): Result<MessagesData> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyOnline()) {
            val cached = cache.getMessages(channel)
            return@withContext if (cached.isNotEmpty()) {
                Result.Success(MessagesData(fromCache = true, canLoadMore = canLoadMore(channel)))
            } else {
                Result.Error(MSG_OFFLINE_NO_CACHE)
            }
        }

        val oldestId = cache.getOldestServerId(channel)
            ?: return@withContext Result.Success(MessagesData(fromCache = false, canLoadMore = false))

        fetchAndCacheMessages(channel, oldestId, reverse = true)
    }

    suspend fun sendTextMessage(channel: String, text: String): Result<SendMessageData> =
        withContext(Dispatchers.IO) {
            val username = sessionManager.username.orEmpty()
            val localId = "local_${UUID.randomUUID()}"
            val pending = MessageEntity(
                id = localId,
                channelName = channel,
                from = username,
                to = channel,
                time = null,
                textBody = text.trim(),
                imageLink = null,
                isPending = true,
                sortKey = System.currentTimeMillis(),
            )
            cache.upsertMessages(listOf(pending))

            if (!networkMonitor.isCurrentlyOnline()) {
                return@withContext Result.Success(SendMessageData(queued = true))
            }

            when (val sendResult = sendPendingMessage(pending)) {
                is Result.Success -> {
                    refreshMessages(channel)
                    Result.Success(SendMessageData(queued = false))
                }
                is Result.Error -> sendResult
            }
        }

    /** Отправляет все сообщения из очереди при появлении сети */
    suspend fun syncPendingMessages(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyOnline()) {
            return@withContext Result.Success(Unit)
        }

        val pending = cache.getAllPending()
        val affectedChannels = mutableSetOf<String>()

        for (message in pending) {
            when (sendPendingMessage(message)) {
                is Result.Success -> affectedChannels.add(message.channelName)
                is Result.Error -> {
                    // останавливаемся на первой ошибке, остальные попробуем позже
                    break
                }
            }
        }

        for (channel in affectedChannels) {
            refreshMessages(channel)
        }
        Result.Success(Unit)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cache.clearAll()
    }

    private suspend fun cachedChannelsOrError(): Result<ChannelsData> {
        val cached = cache.getChannels()
        return if (cached.isNotEmpty()) {
            Result.Success(ChannelsData(channels = cached, fromCache = true))
        } else {
            Result.Error(MSG_OFFLINE_NO_CACHE)
        }
    }

    private suspend fun fetchAndCacheMessages(
        channel: String,
        lastKnownId: String,
        reverse: Boolean,
    ): Result<MessagesData> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyOnline()) {
            val cached = cache.getMessages(channel)
            return@withContext if (cached.isNotEmpty()) {
                Result.Success(MessagesData(fromCache = true, canLoadMore = canLoadMore(channel)))
            } else {
                Result.Error(MSG_OFFLINE_NO_CACHE)
            }
        }

        try {
            val remote = api.getChannelMessages(
                channelName = channel,
                limit = FaeryTeaApi.DEFAULT_LIMIT,
                lastKnownId = lastKnownId,
                reverse = reverse,
            )
            val entities = remote.toEntities(channel)
            cache.upsertMessages(entities)
            Result.Success(
                MessagesData(
                    fromCache = false,
                    canLoadMore = remote.size >= FaeryTeaApi.DEFAULT_LIMIT,
                ),
            )
        } catch (e: FaeryTeaHttpException) {
            if (e.code == HTTP_UNAUTHORIZED) {
                mapHttpError(e)
            } else {
                fallbackMessages(channel)
            }
        } catch (_: IOException) {
            fallbackMessages(channel)
        }
    }

    private suspend fun fallbackMessages(channel: String): Result<MessagesData> {
        val cached = cache.getMessages(channel)
        return if (cached.isNotEmpty()) {
            Result.Success(MessagesData(fromCache = true, canLoadMore = canLoadMore(channel)))
        } else {
            Result.Error(MSG_NETWORK_ERROR)
        }
    }

    private suspend fun canLoadMore(channel: String): Boolean {
        val syncedCount = cache.countSyncedByChannel(channel)
        return syncedCount >= FaeryTeaApi.DEFAULT_LIMIT
    }

    private suspend fun sendPendingMessage(pending: MessageEntity): Result<Unit> {
        val text = pending.textBody.orEmpty()
        if (text.isBlank()) {
            cache.deleteMessageById(pending.id)
            return Result.Success(Unit)
        }
        return try {
            val body = OutgoingMessageDto(
                from = pending.from,
                to = pending.channelName,
                data = OutgoingContentDto(TextPayloadDto(text)),
            )
            val response = api.sendMessage(body)
            response.use {
                if (it.isSuccessful) {
                    cache.deleteMessageById(pending.id)
                    Result.Success(Unit)
                } else {
                    Result.Error(MSG_SEND_FAILED.format(it.code))
                }
            }
        } catch (e: FaeryTeaHttpException) {
            mapHttpError(e)
        } catch (_: IOException) {
            Result.Error(MSG_NETWORK_ERROR)
        }
    }

    private fun mapHttpError(e: FaeryTeaHttpException): Result.Error =
        if (e.code == HTTP_UNAUTHORIZED) {
            Result.Error(MSG_SESSION_EXPIRED, isUnauthorized = true)
        } else {
            Result.Error(MSG_SERVER_ERROR.format(e.code))
        }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val MSG_EMPTY_TOKEN = "empty_token"
        private const val MSG_WRONG_CREDENTIALS = "wrong_credentials"
        private const val MSG_LOGIN_FAILED = "login_failed_%d"
        private const val MSG_NETWORK_ERROR = "network_error"
        private const val MSG_OFFLINE_NO_CACHE = "offline_no_cache"
        private const val MSG_SEND_FAILED = "send_failed_%d"
        private const val MSG_SERVER_ERROR = "server_error_%d"
        private const val MSG_SESSION_EXPIRED = "session_expired"
    }
}
