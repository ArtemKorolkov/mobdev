package com.student.mobile_dev_laboratory_work_3.ui.main

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.student.mobile_dev_laboratory_work_3.data.repository.ChatRepository

/** Фабрика ViewModel с SavedStateHandle для сохранения состояния при повороте */
class ChatViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val repository: ChatRepository,
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
