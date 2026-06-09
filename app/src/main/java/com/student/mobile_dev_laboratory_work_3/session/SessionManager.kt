package com.student.mobile_dev_laboratory_work_3.session

import android.content.Context
import androidx.core.content.edit

/**
 * Хранит учётные данные и токен авторизации.
 * Логин и пароль сохраняются для автоматического входа (требование задания).
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit { putString(KEY_PASSWORD, value) }

    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    /** Есть сохранённые логин и пароль — можно пропустить экран входа */
    fun hasStoredCredentials(): Boolean =
        !username.isNullOrBlank() && !password.isNullOrBlank()

    fun isLoggedIn(): Boolean = !authToken.isNullOrBlank()

    fun saveCredentials(name: String, pwd: String, token: String) {
        onLoginSuccess(name, pwd, token)
    }

    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    /** После явного выхода не выполнять автоматический вход */
    var skipAutoLogin: Boolean
        get() = prefs.getBoolean(KEY_SKIP_AUTO, false)
        set(value) = prefs.edit { putBoolean(KEY_SKIP_AUTO, value) }

    fun clearAll() {
        prefs.edit { clear() }
    }

    fun onLogout() {
        prefs.edit {
            remove(KEY_TOKEN)
            putBoolean(KEY_SKIP_AUTO, true)
        }
    }

    fun onLoginSuccess(name: String, pwd: String, token: String) {
        prefs.edit {
            putString(KEY_USERNAME, name)
            putString(KEY_PASSWORD, pwd)
            putString(KEY_TOKEN, token)
            putBoolean(KEY_SKIP_AUTO, false)
        }
    }

    companion object {
        private const val PREFS_NAME = "faerytea_session"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_TOKEN = "token"
        private const val KEY_SKIP_AUTO = "skip_auto_login"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
    }
}
