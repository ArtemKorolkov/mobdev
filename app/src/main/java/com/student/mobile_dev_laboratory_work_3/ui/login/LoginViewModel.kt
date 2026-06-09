package com.student.mobile_dev_laboratory_work_3.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.student.mobile_dev_laboratory_work_3.data.model.LoginUiState
import com.student.mobile_dev_laboratory_work_3.data.repository.ChatRepository
import com.student.mobile_dev_laboratory_work_3.data.repository.Result
import kotlinx.coroutines.launch

/** ViewModel экрана входа */
class LoginViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(name: String, password: String) {
        if (name.isBlank() || password.isBlank()) return
        _uiState.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.login(name.trim(), password)) {
                is Result.Success -> {
                    _uiState.value = LoginUiState()
                    _loginSuccess.value = true
                }
                is Result.Error -> {
                    _uiState.value = LoginUiState(errorMessage = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    fun resetLoginSuccess() {
        _loginSuccess.value = false
    }
}

class LoginViewModelFactory(
    private val repository: ChatRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
