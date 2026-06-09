package com.student.mobile_dev_laboratory_work_3.data.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.student.mobile_dev_laboratory_work_3.data.model.LoginRequest
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.OutgoingMessageDto
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** REST API сервера https://faerytea.name/ на чистом OkHttp */
class FaeryTeaApi(
    private val client: OkHttpClient,
    private val gson: Gson,
) {

    suspend fun login(request: LoginRequest): Response {
        val body = gson.toJson(request).toRequestBody(JSON_MEDIA_TYPE)
        return execute(
            Request.Builder()
                .url(baseUrl.resolve("login")!!)
                .post(body)
                .build(),
        )
    }

    suspend fun logout(): Response =
        execute(
            Request.Builder()
                .url(baseUrl.resolve("logout")!!)
                .post("".toRequestBody())
                .build(),
        )

    suspend fun getChannels(): List<String> {
        val response = execute(
            Request.Builder()
                .url(baseUrl.resolve("channels")!!)
                .get()
                .build(),
        )
        response.use {
            if (!it.isSuccessful) {
                throw FaeryTeaHttpException(it.code)
            }
            val json = it.body?.string().orEmpty()
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(json, type)
        }
    }

    suspend fun getChannelMessages(
        channelName: String,
        limit: Int = DEFAULT_LIMIT,
        lastKnownId: String = "0",
        reverse: Boolean = false,
    ): List<MessageDto> {
        val url = baseUrl.newBuilder()
            .addPathSegments("channel")
            .addPathSegment(channelName)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("lastKnownId", lastKnownId)
            .addQueryParameter("reverse", reverse.toString())
            .build()

        val response = execute(Request.Builder().url(url).get().build())
        response.use {
            if (!it.isSuccessful) {
                throw FaeryTeaHttpException(it.code)
            }
            val json = it.body?.string().orEmpty()
            val type = object : TypeToken<List<MessageDto>>() {}.type
            return gson.fromJson(json, type)
        }
    }

    suspend fun sendMessage(message: OutgoingMessageDto): Response {
        val body = gson.toJson(message).toRequestBody(JSON_MEDIA_TYPE)
        return execute(
            Request.Builder()
                .url(baseUrl.resolve("messages")!!)
                .post(body)
                .build(),
        )
    }

    private suspend fun execute(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isCancelled) return
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (continuation.isCancelled) {
                            response.close()
                            return
                        }
                        continuation.resume(response)
                    }
                },
            )
        }

    companion object {
        const val BASE_URL = "https://faerytea.name/"
        const val DEFAULT_LIMIT = 20

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val baseUrl = BASE_URL.toHttpUrl()
    }
}
