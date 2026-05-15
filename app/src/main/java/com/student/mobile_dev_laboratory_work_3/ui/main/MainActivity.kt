package com.student.mobile_dev_laboratory_work_3.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.student.mobile_dev_laboratory_work_3.FaeryTeaApp
import com.student.mobile_dev_laboratory_work_3.R
import com.student.mobile_dev_laboratory_work_3.data.api.ApiClient
import com.student.mobile_dev_laboratory_work_3.data.model.PortraitScreen
import com.student.mobile_dev_laboratory_work_3.databinding.ActivityMainBinding
import com.student.mobile_dev_laboratory_work_3.ui.login.LoginActivity

/**
 * Главный экран после входа.
 * Портрет: один контейнер — чаты / сообщения / картинка.
 * Ландшафт: слева чаты, справа сообщения или заглушка.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { FaeryTeaApp.from(application) }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(this, app.repository)
    }

    val chatViewModelFactory: ChatViewModelFactory
        get() = ChatViewModelFactory(this, app.repository)

    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ApiClient.setUnauthorizedListener {
            runOnUiThread { navigateToLogin() }
        }

        if (!app.sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setupBackPressed()
        restoreUi()
    }

    /** Восстанавливает фрагменты после поворота без сетевых запросов (данные в ViewModel) */
    private fun restoreUi() {
        if (isLandscape) {
            ensureFragment(R.id.container_chats, ChatsFragment::class.java) {
                ChatsFragment.newInstance()
            }
            val rightFragment: Fragment = when {
                !viewModel.imagePath.value.isNullOrBlank() -> ImageFragment.newInstance()
                viewModel.selectedChannel != null -> MessagesFragment.newInstance()
                else -> MessagesPlaceholderFragment.newInstance()
            }
            ensureFragment(R.id.container_messages, rightFragment.javaClass) { rightFragment }
        } else {
            val screen = viewModel.portraitScreen.value ?: PortraitScreen.CHATS
            showPortraitScreen(screen)
        }
    }

    private fun ensureFragment(containerId: Int, tagClass: Class<out Fragment>, factory: () -> Fragment) {
        val existing = supportFragmentManager.findFragmentById(containerId)
        if (existing == null || !tagClass.isInstance(existing)) {
            supportFragmentManager.commit {
                replace(containerId, factory())
            }
        }
    }

    fun showPortraitScreen(screen: PortraitScreen) {
        if (isLandscape) return
        val fragment: Fragment = when (screen) {
            PortraitScreen.CHATS -> ChatsFragment.newInstance()
            PortraitScreen.MESSAGES -> MessagesFragment.newInstance()
            PortraitScreen.IMAGE -> ImageFragment.newInstance()
        }
        supportFragmentManager.commit {
            replace(R.id.container_portrait, fragment)
        }
    }

    fun onChannelSelected(channel: String) {
        viewModel.selectChannel(channel)
        if (isLandscape) {
            supportFragmentManager.commit {
                replace(R.id.container_messages, MessagesFragment.newInstance())
            }
        } else {
            showPortraitScreen(PortraitScreen.MESSAGES)
        }
    }

    fun onImageOpened() {
        if (isLandscape) {
            supportFragmentManager.commit {
                replace(R.id.container_messages, ImageFragment.newInstance())
            }
        } else {
            showPortraitScreen(PortraitScreen.IMAGE)
        }
    }

    fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    !isLandscape && viewModel.portraitScreen.value == PortraitScreen.IMAGE -> {
                        viewModel.closeImage()
                        showPortraitScreen(PortraitScreen.MESSAGES)
                    }
                    !isLandscape && viewModel.portraitScreen.value == PortraitScreen.MESSAGES -> {
                        viewModel.showChatsScreen()
                        showPortraitScreen(PortraitScreen.CHATS)
                    }
                    isLandscape && !viewModel.imagePath.value.isNullOrBlank() -> {
                        viewModel.closeImage()
                        restoreUi()
                    }
                    isLandscape && viewModel.selectedChannel != null -> {
                        viewModel.closeChatInLandscape()
                        supportFragmentManager.commit {
                            replace(R.id.container_messages, MessagesPlaceholderFragment.newInstance())
                        }
                    }
                    else -> finish()
                }
            }
        })
    }

    override fun onDestroy() {
        if (isFinishing) {
            ApiClient.clearUnauthorizedListener()
        }
        super.onDestroy()
    }
}
