package com.student.mobile_dev_laboratory_work_3.util

import android.content.Context
import com.student.mobile_dev_laboratory_work_3.R

/** Преобразует коды ошибок репозитория в строки для пользователя */
object ErrorMapper {

    fun toUserMessage(context: Context, errorCode: String): String {
        return when {
            errorCode == "wrong_credentials" -> context.getString(R.string.error_wrong_credentials)
            errorCode == "network_error" -> context.getString(R.string.error_network)
            errorCode == "offline_no_cache" -> context.getString(R.string.error_offline_no_cache)
            errorCode == "session_expired" -> context.getString(R.string.error_session_expired)
            errorCode == "empty_token" -> context.getString(R.string.error_login_failed)
            errorCode.startsWith("login_failed_") -> context.getString(R.string.error_login_failed)
            errorCode.startsWith("send_failed_") -> context.getString(R.string.error_send_failed)
            errorCode.startsWith("server_error_") -> {
                val code = errorCode.removePrefix("server_error_")
                context.getString(R.string.error_server, code)
            }
            else -> context.getString(R.string.error_unknown)
        }
    }
}
