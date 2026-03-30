package com.example.frontend.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend.core.session.SessionManager
import com.example.frontend.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUser: StateFlow<User?> = sessionManager.currentUser

    fun fetchCurrentUser() {
        viewModelScope.launch {
            sessionManager.fetchCurrentUser()
        }
    }

    fun clearSession() {
        sessionManager.clearSession()
    }
}
