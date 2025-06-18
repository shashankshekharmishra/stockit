package com.example.stockit.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.example.stockit.utils.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel()