package com.example.frontend.core.session

import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.network.WebSocketManager
import com.example.frontend.domain.model.User
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val getMeUseCase: GetMeUseCase,
    private val webSocketManager: WebSocketManager
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun fetchCurrentUser(isRefresh: Boolean = false) {
        when (val result = getMeUseCase(isRefresh)) {
            is ApiResult.Success -> {
                _currentUser.value = result.data
                webSocketManager.connect()
            }
            is ApiResult.Error -> {
                _currentUser.value = null
            }
        }
    }

    fun clearSession() {
        _currentUser.value = null
    }
}
