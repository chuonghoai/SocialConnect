package com.example.frontend.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.network.ApiResult
import com.example.frontend.core.network.TokenProvider
import com.example.frontend.domain.usecase.AuthUseCase.LogoutUseCase
import com.example.frontend.domain.usecase.UserUseCase.GetMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val getMeUseCase: GetMeUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val token = tokenProvider.getAccessToken()
            if (token.isNullOrBlank()) {
                _startDestination.value = Routes.LOGIN
                return@launch
            }

            when (val meResult = getMeUseCase()) {
                is ApiResult.Success -> {
                    val isAdmin = meResult.data.role.equals("ADMIN", ignoreCase = true)
                    _startDestination.value = if (isAdmin) Routes.ADMIN else Routes.HOME
                }

                is ApiResult.Error -> {
                    if (meResult.code == 401 || meResult.code == 403) {
                        runCatching { logoutUseCase() }
                        _startDestination.value = Routes.LOGIN
                    } else {
                        _startDestination.value = Routes.HOME
                    }
                }
            }
        }
    }
}
