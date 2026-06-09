package com.student.mobile_dev_laboratory_work_3.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.mobile_dev_laboratory_work_3.data.model.ChannelsUiState
import com.student.mobile_dev_laboratory_work_3.data.model.MessagesUiState
import com.student.mobile_dev_laboratory_work_3.data.model.PortraitScreen
import com.student.mobile_dev_laboratory_work_3.data.repository.ChatRepository
import com.student.mobile_dev_laboratory_work_3.data.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel главного экрана.
 * Сообщения и каналы читаются из Room; навигация — в SavedStateHandle.
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _channelsState = MutableLiveData(ChannelsUiState())
    val channelsState: LiveData<ChannelsUiState> = _channelsState

    private val _messagesState = MutableLiveData(MessagesUiState())
    val messagesState: LiveData<MessagesUiState> = _messagesState

    private val _portraitScreen = MutableLiveData(
        savedStateHandle.get<String>(KEY_PORTRAIT_SCREEN)?.let { PortraitScreen.valueOf(it) }
            ?: PortraitScreen.CHATS,
    )
    val portraitScreen: LiveData<PortraitScreen> = _portraitScreen

    private val _imagePath = MutableLiveData(savedStateHandle.get<String>(KEY_IMAGE_PATH))
    val imagePath: LiveData<String?> = _imagePath

    val selectedChannel: String?
        get() = savedStateHandle.get<String>(KEY_SELECTED_CHANNEL)

    private var channelsJob: Job? = null
    private var messagesJob: Job? = null
    private var channelsLoaded = false

    init {
        observeNetwork()
        observeChannelsFromCache()
        selectedChannel?.let { startMessagesObservation(it) }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            repository.observeNetworkOnline().collect { online ->
                val offline = !online
                _channelsState.value = _channelsState.value?.copy(isOffline = offline)
                _messagesState.value = _messagesState.value?.copy(isOffline = offline)

                if (online) {
                    repository.syncPendingMessages()
                    if (channelsLoaded) {
                        refreshChannelsFromNetwork()
                    }
                    selectedChannel?.let { refreshMessagesFromNetwork(it) }
                }
            }
        }
    }

    private fun observeChannelsFromCache() {
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            repository.observeChannels().collectLatest { channels ->
                if (channels.isNotEmpty()) {
                    _channelsState.value = (_channelsState.value ?: ChannelsUiState()).copy(
                        channels = channels,
                    )
                }
            }
        }
    }

    private fun startMessagesObservation(channel: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeMessages(channel).collectLatest { messages ->
                _messagesState.value = (_messagesState.value ?: MessagesUiState()).copy(
                    messages = messages,
                )
            }
        }
    }

    fun loadChannelsIfNeeded() {
        if (channelsLoaded) return
        _channelsState.value = _channelsState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.getChannels()) {
                is Result.Success -> {
                    channelsLoaded = true
                    _channelsState.value = ChannelsUiState(
                        channels = result.data.channels,
                        isOffline = _channelsState.value?.isOffline == true,
                        isShowingCache = result.data.fromCache,
                    )
                }
                is Result.Error -> {
                    _channelsState.value = ChannelsUiState(
                        channels = _channelsState.value?.channels.orEmpty(),
                        isOffline = _channelsState.value?.isOffline == true,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun refreshChannelsFromNetwork() {
        viewModelScope.launch {
            when (val result = repository.getChannels()) {
                is Result.Success -> {
                    channelsLoaded = true
                    _channelsState.value = (_channelsState.value ?: ChannelsUiState()).copy(
                        channels = result.data.channels,
                        isShowingCache = result.data.fromCache,
                        errorMessage = null,
                    )
                }
                is Result.Error -> {
                    if (result.message != "offline_no_cache") {
                        _channelsState.value = _channelsState.value?.copy(errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun selectChannel(channel: String) {
        savedStateHandle[KEY_SELECTED_CHANNEL] = channel
        _portraitScreen.value = PortraitScreen.MESSAGES
        savedStateHandle[KEY_PORTRAIT_SCREEN] = PortraitScreen.MESSAGES.name

        startMessagesObservation(channel)
        refreshMessagesFromNetwork(channel, showLoading = true)
    }

    private fun refreshMessagesFromNetwork(channel: String, showLoading: Boolean = false) {
        if (showLoading) {
            _messagesState.value = (_messagesState.value ?: MessagesUiState()).copy(isLoading = true)
        }
        viewModelScope.launch {
            when (val result = repository.refreshMessages(channel)) {
                is Result.Success -> {
                    _messagesState.value = (_messagesState.value ?: MessagesUiState()).copy(
                        isLoading = false,
                        canLoadMore = result.data.canLoadMore,
                        isShowingCache = result.data.fromCache,
                        errorMessage = null,
                    )
                }
                is Result.Error -> {
                    _messagesState.value = (_messagesState.value ?: MessagesUiState()).copy(
                        isLoading = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun loadOlderMessages() {
        val channel = selectedChannel ?: return
        val current = _messagesState.value ?: return
        if (current.isLoadingMore || !current.canLoadMore) return

        _messagesState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            when (val result = repository.loadOlderMessages(channel)) {
                is Result.Success -> {
                    _messagesState.value = current.copy(
                        isLoadingMore = false,
                        canLoadMore = result.data.canLoadMore,
                        isShowingCache = result.data.fromCache,
                    )
                }
                is Result.Error -> {
                    _messagesState.value = current.copy(
                        isLoadingMore = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val channel = selectedChannel ?: return
        if (text.isBlank()) return

        val current = _messagesState.value ?: MessagesUiState()
        _messagesState.value = current.copy(sendInProgress = true)
        viewModelScope.launch {
            when (val result = repository.sendTextMessage(channel, text.trim())) {
                is Result.Success -> {
                    _messagesState.value = current.copy(sendInProgress = false)
                    if (!result.data.queued) {
                        refreshMessagesFromNetwork(channel)
                    }
                }
                is Result.Error -> {
                    _messagesState.value = current.copy(
                        sendInProgress = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun openImage(path: String) {
        _imagePath.value = path
        savedStateHandle[KEY_IMAGE_PATH] = path
        _portraitScreen.value = PortraitScreen.IMAGE
        savedStateHandle[KEY_PORTRAIT_SCREEN] = PortraitScreen.IMAGE.name
    }

    fun closeImage() {
        _imagePath.value = null
        savedStateHandle.remove<String>(KEY_IMAGE_PATH)
        _portraitScreen.value = PortraitScreen.MESSAGES
        savedStateHandle[KEY_PORTRAIT_SCREEN] = PortraitScreen.MESSAGES.name
    }

    fun showChatsScreen() {
        _portraitScreen.value = PortraitScreen.CHATS
        savedStateHandle[KEY_PORTRAIT_SCREEN] = PortraitScreen.CHATS.name
    }

    fun closeChatInLandscape() {
        messagesJob?.cancel()
        messagesJob = null
        savedStateHandle.remove<String>(KEY_SELECTED_CHANNEL)
        _messagesState.value = MessagesUiState(isOffline = _messagesState.value?.isOffline == true)
    }

    fun clearMessagesError() {
        _messagesState.value = _messagesState.value?.copy(errorMessage = null)
    }

    fun clearChannelsError() {
        _channelsState.value = _channelsState.value?.copy(errorMessage = null)
    }

    companion object {
        const val KEY_SELECTED_CHANNEL = "selected_channel"
        private const val KEY_PORTRAIT_SCREEN = "portrait_screen"
        private const val KEY_IMAGE_PATH = "image_path"
    }
}
