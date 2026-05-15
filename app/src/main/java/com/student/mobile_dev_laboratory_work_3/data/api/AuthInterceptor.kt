package com.student.mobile_dev_laboratory_work_3.data.api

import com.student.mobile_dev_laboratory_work_3.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/** Добавляет заголовок X-Auth-Token ко всем запросам, где он нужен */
class AuthInterceptor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        sessionManager.authToken?.let { token ->
            requestBuilder.header(HEADER_AUTH, token)
        }
        return chain.proceed(requestBuilder.build())
    }

    companion object {
        const val HEADER_AUTH = "X-Auth-Token"
    }
}
