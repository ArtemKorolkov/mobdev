package com.student.mobile_dev_laboratory_work_3.data.model

/** Неизменяемое состояние экрана входа */
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** Сообщение для UI: данные + признак ожидания отправки */
data class ChatMessage(
    val dto: MessageDto,
    val isPending: Boolean = false,
)

/** Неизменяемое состояние списка каналов */
data class ChannelsUiState(
    val channels: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isShowingCache: Boolean = false,
    val errorMessage: String? = null,
)

/** Неизменяемое состояние экрана сообщений */
data class MessagesUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val isOffline: Boolean = false,
    val isShowingCache: Boolean = false,
    val errorMessage: String? = null,
    val sendInProgress: Boolean = false,
)

/** Экран, отображаемый в портретной ориентации */
enum class PortraitScreen {
    CHATS,
    MESSAGES,
    IMAGE,
}
