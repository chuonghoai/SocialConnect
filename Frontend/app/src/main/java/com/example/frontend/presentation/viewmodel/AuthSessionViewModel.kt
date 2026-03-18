package com.example.frontend.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.frontend.core.network.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthSessionViewModel @Inject constructor(
    authSessionManager: AuthSessionManager
) : ViewModel() {
    val sessionExpiredEvents = authSessionManager.sessionExpiredEvents
}
