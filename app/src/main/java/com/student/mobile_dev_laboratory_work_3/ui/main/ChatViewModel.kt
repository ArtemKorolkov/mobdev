package com.student.mobile_dev_laboratory_work_3.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.mobile_dev_laboratory_work_3.data.model.ChannelsUiState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.data.model.MessagesUiState
import com.student.mobile_dev_laboratory_work_3.data.model.PortraitScreen
import com.student.mobile_dev_laboratory_work_3.data.repository.ChatRepository
import com.student.mobile_dev_laboratory_work_3.data.repository.Result
import com.student.mobile_dev_laboratory_work_3.data.api.FaeryTeaApi
import kotlinx.coroutines.launch

/**
 * Общий ViewModel главного экрана.
 * Сохраняет выбранный чат и сообщения в SavedStateHandle — при повороте сеть не дергается.
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _channelsState = MutableLiveData(ChannelsUiState())
    val channelsState: LiveData<ChannelsUiState> = _channelsState

    private val _messagesState = MutableLiveData(restoreMessagesState())
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

    private var channelsLoaded = false
    private var messagesLoadedForChannel: String? =
        savedStateHandle.get<String>(KEY_SELECTED_CHANNEL)?.takeIf {
            !restoreMessagesList().isNullOrEmpty()
        }

    private fun restoreMessagesState(): MessagesUiState {
        val cached = restoreMessagesList()
        return if (cached.isNullOrEmpty()) {
            MessagesUiState()
        } else {
            MessagesUiState(messages = cached, canLoadMore = cached.size >= FaeryTeaApi.DEFAULT_LIMIT)
        }
    }

    private val gson = Gson()

    private fun restoreMessagesList(): List<MessageDto>? {
        val json = savedStateHandle.get<String>(KEY_MESSAGES) ?: return null
        val type = object : TypeToken<List<MessageDto>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun persistMessages(messages: List<MessageDto>) {
        savedStateHandle[KEY_MESSAGES] = gson.toJson(messages)
    }

    fun loadChannelsIfNeeded() {
        if (channelsLoaded) return
        _channelsState.value = _channelsState.value?.copy(isLoading = true) ?: ChannelsUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.getChannels()) {
                is Result.Success -> {
                    channelsLoaded = true
                    _channelsState.value = ChannelsUiState(channels = result.data)
                }
                is Result.Error -> {
                    _channelsState.value = ChannelsUiState(errorMessage = result.message)
                }
            }
        }
    }

    fun selectChannel(channel: String) {
        savedStateHandle[KEY_SELECTED_CHANNEL] = channel
        _portraitScreen.value = PortraitScreen.MESSAGES
        savedStateHandle[KEY_PORTRAIT_SCREEN] = PortraitScreen.MESSAGES.name

        if (messagesLoadedForChannel == channel) return

        messagesLoadedForChannel = channel
        _messagesState.value = MessagesUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.loadMessages(channel)) {
                is Result.Success -> {
                    val sorted = result.data.sortedBy { it.id.toLongOrNull() ?: 0L }
                    val state = MessagesUiState(
                        messages = sorted,
                        canLoadMore = result.data.size >= FaeryTeaApi.DEFAULT_LIMIT,
                    )
                    persistMessages(sorted)
                    _messagesState.value = state
                }
                is Result.Error -> {
                    messagesLoadedForChannel = null
                    _messagesState.value = MessagesUiState(errorMessage = result.message)
                }
            }
        }
    }

    fun loadOlderMessages() {
        val channel = selectedChannel ?: return
        val current = _messagesState.value ?: return
        if (current.isLoadingMore || !current.canLoadMore) return

        val oldestId = current.messages.minOfOrNull { it.id.toLongOrNull() ?: Long.MAX_VALUE }?.toString()
            ?: return

        _messagesState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            when (val result = repository.loadOlderMessages(channel, oldestId)) {
                is Result.Success -> {
                    val older = result.data.sortedBy { it.id.toLongOrNull() ?: 0L }
                    val merged = (older + current.messages).distinctBy { it.id }
                        .sortedBy { it.id.toLongOrNull() ?: 0L }
                    persistMessages(merged)
                    _messagesState.value = current.copy(
                        messages = merged,
                        isLoadingMore = false,
                        canLoadMore = result.data.size >= FaeryTeaApi.DEFAULT_LIMIT,
                    )
                }
                is Result.Error -> {
                    _messagesState.value = current.copy(isLoadingMore = false, errorMessage = result.message)
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
                is Result.Success -> reloadMessagesAfterSend(channel)
                is Result.Error -> {
                    _messagesState.value = current.copy(sendInProgress = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun reloadMessagesAfterSend(channel: String) {
        viewModelScope.launch {
            when (val result = repository.loadMessages(channel)) {
                is Result.Success -> {
                    val sorted = result.data.sortedBy { it.id.toLongOrNull() ?: 0L }
                    val state = MessagesUiState(
                        messages = sorted,
                        canLoadMore = result.data.size >= FaeryTeaApi.DEFAULT_LIMIT,
                    )
                    persistMessages(sorted)
                    _messagesState.value = state
                }
                is Result.Error -> {
                    _messagesState.value = MessagesUiState(errorMessage = result.message)
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
        savedStateHandle.remove<String>(KEY_SELECTED_CHANNEL)
        savedStateHandle.remove<String>(KEY_MESSAGES)
        messagesLoadedForChannel = null
        _messagesState.value = MessagesUiState()
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
        private const val KEY_MESSAGES = "messages_cache"
    }
}
