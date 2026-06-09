package com.student.mobile_dev_laboratory_work_3.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.student.mobile_dev_laboratory_work_3.session.SessionManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Фабрика OkHttp-клиента и API-слоя */
object ApiClient {

    private var unauthorizedListener: (() -> Unit)? = null

    fun setUnauthorizedListener(listener: () -> Unit) {
        unauthorizedListener = listener
    }

    fun clearUnauthorizedListener() {
        unauthorizedListener = null
    }

    fun createApi(sessionManager: SessionManager): FaeryTeaApi {
        val gson = createGson()
        return FaeryTeaApi(createHttpClient(sessionManager), gson)
    }

    fun createImageOkHttpClient(sessionManager: SessionManager): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

    private fun createHttpClient(sessionManager: SessionManager): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(
                UnauthorizedInterceptor(sessionManager) {
                    unauthorizedListener?.invoke()
                },
            )
            .build()

    private fun createGson(): Gson = GsonBuilder().create()

    private const val TIMEOUT_SEC = 30L
}
