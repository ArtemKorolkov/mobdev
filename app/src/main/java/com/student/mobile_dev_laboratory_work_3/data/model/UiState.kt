package com.student.mobile_dev_laboratory_work_3.data.model

/** Неизменяемое состояние экрана входа */
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** Неизменяемое состояние списка каналов */
data class ChannelsUiState(
    val channels: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** Неизменяемое состояние экрана сообщений */
data class MessagesUiState(
    val messages: List<MessageDto> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
    val sendInProgress: Boolean = false,
)

/** Экран, отображаемый в портретной ориентации */
enum class PortraitScreen {
    CHATS,
    MESSAGES,
    IMAGE,
}
