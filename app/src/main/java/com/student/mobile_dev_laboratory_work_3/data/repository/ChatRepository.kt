package com.student.mobile_dev_laboratory_work_3.data.repository

import com.student.mobile_dev_laboratory_work_3.data.api.FaeryTeaApi
import com.student.mobile_dev_laboratory_work_3.data.model.LoginRequest
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.OutgoingContentDto
import com.student.mobile_dev_laboratory_work_3.data.model.OutgoingMessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.TextPayloadDto
import com.student.mobile_dev_laboratory_work_3.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/** Результат сетевой операции */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val isUnauthorized: Boolean = false) : Result<Nothing>()
}

/**
 * Репозиторий: вся работа с сетью в одном месте.
 * ViewModel вызывает методы репозитория, не Retrofit напрямую.
 */
class ChatRepository(
    private val api: FaeryTeaApi,
    private val sessionManager: SessionManager,
) {

    suspend fun login(name: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(name, password))
            when {
                response.isSuccessful -> {
                    val token = response.body()?.string()?.trim().orEmpty()
                    if (token.isEmpty()) {
                        Result.Error(MSG_EMPTY_TOKEN)
                    } else {
                        sessionManager.saveCredentials(name, password, token)
                        Result.Success(Unit)
                    }
                }
                response.code() == HTTP_UNAUTHORIZED ->
                    Result.Error(MSG_WRONG_CREDENTIALS, isUnauthorized = false)
                else -> Result.Error(MSG_LOGIN_FAILED.format(response.code()))
            }
        } catch (_: HttpException) {
            Result.Error(MSG_NETWORK_ERROR)
        } catch (_: IOException) {
            Result.Error(MSG_NETWORK_ERROR)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.logout()
            sessionManager.clearToken()
            Result.Success(Unit)
        } catch (_: Exception) {
            sessionManager.clearToken()
            Result.Success(Unit)
        }
    }

    suspend fun getChannels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Result.Success(api.getChannels())
        } catch (e: HttpException) {
            mapHttpError(e)
        } catch (_: IOException) {
            Result.Error(MSG_NETWORK_ERROR)
        }
    }

    /** Первая страница сообщений (не более 20) */
    suspend fun loadMessages(channel: String): Result<List<MessageDto>> =
        fetchMessages(channel, lastKnownId = "0", reverse = false)

    /** Подгрузка более старых сообщений */
    suspend fun loadOlderMessages(channel: String, oldestId: String): Result<List<MessageDto>> =
        fetchMessages(channel, lastKnownId = oldestId, reverse = true)

    private suspend fun fetchMessages(
        channel: String,
        lastKnownId: String,
        reverse: Boolean,
    ): Result<List<MessageDto>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getChannelMessages(
                channelName = channel,
                limit = FaeryTeaApi.DEFAULT_LIMIT,
                lastKnownId = lastKnownId,
                reverse = reverse,
            )
            Result.Success(list)
        } catch (e: HttpException) {
            mapHttpError(e)
        } catch (_: IOException) {
            Result.Error(MSG_NETWORK_ERROR)
        }
    }

    suspend fun sendTextMessage(channel: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val username = sessionManager.username.orEmpty()
            try {
                val body = OutgoingMessageDto(
                    from = username,
                    to = channel,
                    data = OutgoingContentDto(TextPayloadDto(text)),
                )
                val response = api.sendMessage(body)
                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    Result.Error(MSG_SEND_FAILED.format(response.code()))
                }
            } catch (e: HttpException) {
                mapHttpError(e)
            } catch (_: IOException) {
                Result.Error(MSG_NETWORK_ERROR)
            }
        }

    private fun mapHttpError(e: HttpException): Result.Error =
        if (e.code() == HTTP_UNAUTHORIZED) {
            Result.Error(MSG_SESSION_EXPIRED, isUnauthorized = true)
        } else {
            Result.Error(MSG_SERVER_ERROR.format(e.code()))
        }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val MSG_EMPTY_TOKEN = "empty_token"
        private const val MSG_WRONG_CREDENTIALS = "wrong_credentials"
        private const val MSG_LOGIN_FAILED = "login_failed_%d"
        private const val MSG_NETWORK_ERROR = "network_error"
        private const val MSG_SEND_FAILED = "send_failed_%d"
        private const val MSG_SERVER_ERROR = "server_error_%d"
        private const val MSG_SESSION_EXPIRED = "session_expired"
    }
}
