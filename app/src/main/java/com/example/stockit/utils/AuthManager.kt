package com.example.stockit.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    // Initialize with current login state
    private val _isLoggedIn = MutableStateFlow(checkCurrentLoginState())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null
        
        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private fun checkCurrentLoginState(): Boolean {
        val hasToken = getAccessToken().isNotEmpty()
        val isValid = sharedPrefs.getBoolean("is_logged_in", false)
        return hasToken && isValid
    }
    
    fun isLoggedIn(): Boolean {
        return checkCurrentLoginState()
    }
    
    fun getAccessToken(): String {
        return sharedPrefs.getString("access_token", "") ?: ""
    }
    
    fun getRefreshToken(): String {
        return sharedPrefs.getString("refresh_token", "") ?: ""
    }
    
    fun getUserId(): String {
        return sharedPrefs.getString("user_id", "") ?: ""
    }
    
    fun getUserEmail(): String {
        return sharedPrefs.getString("user_email", "") ?: ""
    }
    
    fun getUserFullName(): String {
        return sharedPrefs.getString("user_full_name", "") ?: ""
    }
    
    fun getBearerToken(): String {
        val token = getAccessToken()
        return if (token.isNotEmpty()) "Bearer $token" else ""
    }
    
    fun logout() {
        with(sharedPrefs.edit()) {
            clear()
            apply()
        }
        _isLoggedIn.value = false
    }

    // Updated method with immediate state flow update
    fun saveUserData(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userEmail: String,
        userFullName: String
    ) {
        with(sharedPrefs.edit()) {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("user_id", userId)
            putString("user_email", userEmail)
            putString("user_full_name", userFullName)
            putBoolean("is_logged_in", true)
            putLong("token_timestamp", System.currentTimeMillis())
            apply() // Use apply() instead of commit() for async operation
        }
        
        // Force immediate state update
        _isLoggedIn.value = true
        
        // Also refresh the login state to ensure consistency
        refreshLoginState()
    }

    // Add a method to manually refresh the login state
    fun refreshLoginState() {
        val newState = checkCurrentLoginState()
        if (_isLoggedIn.value != newState) {
            _isLoggedIn.value = newState
        }
    }
    
    fun isTokenExpired(): Boolean {
        val timestamp = sharedPrefs.getLong("token_timestamp", 0)
        val thirtyMinutes = 30 * 60 * 1000L // 30 minutes in milliseconds
        return (System.currentTimeMillis() - timestamp) > thirtyMinutes
    }
    
    fun hasValidToken(): Boolean {
        return isLoggedIn() && !isTokenExpired()
    }
}