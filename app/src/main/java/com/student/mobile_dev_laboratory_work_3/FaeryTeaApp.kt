package com.student.mobile_dev_laboratory_work_3

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.student.mobile_dev_laboratory_work_3.data.api.ApiClient
import com.student.mobile_dev_laboratory_work_3.data.api.FaeryTeaApi
import com.student.mobile_dev_laboratory_work_3.data.local.ChatCache
import com.student.mobile_dev_laboratory_work_3.data.network.NetworkMonitor
import com.student.mobile_dev_laboratory_work_3.data.repository.ChatRepository
import com.student.mobile_dev_laboratory_work_3.session.SessionManager

/** Application: сессия, API, кэш, репозиторий */
class FaeryTeaApp : Application(), ImageLoaderFactory {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var repository: ChatRepository
        private set

    lateinit var api: FaeryTeaApi
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager.getInstance(this)
        networkMonitor = NetworkMonitor(this)
        api = ApiClient.createApi(sessionManager)

        val cache = ChatCache(this)
        repository = ChatRepository(
            api = api,
            sessionManager = sessionManager,
            cache = cache,
            networkMonitor = networkMonitor,
        )
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { ApiClient.createImageOkHttpClient(sessionManager) }
            .crossfade(true)
            .build()

    companion object {
        fun from(application: Application): FaeryTeaApp =
            application as FaeryTeaApp
    }
}
