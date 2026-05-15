package com.student.mobile_dev_laboratory_work_3.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.student.mobile_dev_laboratory_work_3.session.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Фабрика Retrofit-клиента и OkHttp с перехватчиками авторизации */
object ApiClient {

    private var unauthorizedListener: (() -> Unit)? = null

    fun setUnauthorizedListener(listener: () -> Unit) {
        unauthorizedListener = listener
    }

    fun clearUnauthorizedListener() {
        unauthorizedListener = null
    }

    fun createApi(sessionManager: SessionManager): FaeryTeaApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(
                UnauthorizedInterceptor(sessionManager) {
                    unauthorizedListener?.invoke()
                },
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(FaeryTeaApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(createGson()))
            .build()
            .create(FaeryTeaApi::class.java)
    }

    fun createImageOkHttpClient(sessionManager: SessionManager): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

    private fun createGson(): Gson = GsonBuilder().create()

    private const val TIMEOUT_SEC = 30L
}
