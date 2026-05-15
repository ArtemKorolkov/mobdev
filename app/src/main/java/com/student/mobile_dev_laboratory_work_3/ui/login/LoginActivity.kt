package com.student.mobile_dev_laboratory_work_3.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.student.mobile_dev_laboratory_work_3.FaeryTeaApp
import com.student.mobile_dev_laboratory_work_3.databinding.ActivityLoginBinding
import com.student.mobile_dev_laboratory_work_3.ui.main.MainActivity
import com.student.mobile_dev_laboratory_work_3.util.ErrorMapper

/**
 * Экран входа: логин, пароль, кнопка «Войти».
 * Пропускается, если логин и пароль уже сохранены — выполняется автоматический вход.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { FaeryTeaApp.from(application) }
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (app.sessionManager.isLoggedIn()) {
            openMainScreen()
            return
        }

        if (savedInstanceState == null &&
            app.sessionManager.hasStoredCredentials() &&
            !app.sessionManager.skipAutoLogin
        ) {
            val name = app.sessionManager.username.orEmpty()
            val pwd = app.sessionManager.password.orEmpty()
            viewModel.login(name, pwd)
        }

        binding.buttonLogin.setOnClickListener {
            viewModel.login(
                binding.editUsername.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty(),
            )
        }

        viewModel.uiState.observe(this) { state ->
            binding.buttonLogin.isEnabled = !state.isLoading
            binding.progressLogin.visibility =
                if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

            state.errorMessage?.let { code ->
                viewModel.clearError()
                AlertDialog.Builder(this)
                    .setMessage(ErrorMapper.toUserMessage(this, code))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                viewModel.resetLoginSuccess()
                openMainScreen()
            }
        }
    }

    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
