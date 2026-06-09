package com.student.mobile_dev_laboratory_work_3.data.api

import com.student.mobile_dev_laboratory_work_3.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * При 401 очищает токен и уведомляет приложение о необходимости повторного входа.
 * Не срабатывает на запросе /login — там 401 означает неверный пароль.
 */
class UnauthorizedInterceptor(
    private val sessionManager: SessionManager,
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_UNAUTHORIZED && !chain.request().url.encodedPath.endsWith(PATH_LOGIN)) {
            sessionManager.clearToken()
            onUnauthorized()
        }
        return response
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val PATH_LOGIN = "/login"
    }
}
