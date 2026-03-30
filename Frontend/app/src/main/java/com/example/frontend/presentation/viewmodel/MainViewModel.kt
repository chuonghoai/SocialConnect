package com.example.frontend.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.frontend.core.util.AppNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val notificationManager: AppNotificationManager
) : ViewModel()