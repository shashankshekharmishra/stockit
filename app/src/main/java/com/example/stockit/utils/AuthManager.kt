package com.example.stockit.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val context: Context) {
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val _isLoggedIn = MutableStateFlow(isLoggedIn())
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
    
    fun isLoggedIn(): Boolean {
        val hasToken = getAccessToken().isNotEmpty()
        val isValid = sharedPrefs.getBoolean("is_logged_in", false)
        return hasToken && isValid
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
            apply()
        }
        _isLoggedIn.value = true
    }

    fun updateLoginStatus(isLoggedIn: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean("is_logged_in", isLoggedIn)
            apply()
        }
        _isLoggedIn.value = isLoggedIn
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