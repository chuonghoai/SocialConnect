package com.example.frontend.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.domain.model.User
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getMeUseCase: GetMeUseCase
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun fetchCurrentUser() {
        viewModelScope.launch {
            when (val result = getMeUseCase()) {
                is ApiResult.Success -> {
                    _currentUser.value = result.data
                }
                is ApiResult.Error -> {
                    _currentUser.value = null
                }
            }
        }
    }

    fun clearSession() {
        _currentUser.value = null
    }
}