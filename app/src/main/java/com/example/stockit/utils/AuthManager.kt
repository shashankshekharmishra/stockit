package com.example.stockit.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class AuthManager(context: Context) {
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    fun isLoggedIn(): Boolean {
        return sharedPrefs.getBoolean("is_logged_in", false)
    }
    
    fun getAccessToken(): String? {
        return sharedPrefs.getString("access_token", null)
    }
    
    fun getRefreshToken(): String? {
        return sharedPrefs.getString("refresh_token", null)
    }
    
    fun getUserId(): String? {
        return sharedPrefs.getString("user_id", null)
    }
    
    fun getUserEmail(): String? {
        return sharedPrefs.getString("user_email", null)
    }
    
    fun getUserFullName(): String? {
        return sharedPrefs.getString("user_full_name", null)
    }
    
    fun logout() {
        with(sharedPrefs.edit()) {
            clear()
            apply()
        }
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
            apply()
        }
    }

    // Overloaded method to accept JSONObject directly
    fun saveUserData(
        accessToken: String,
        refreshToken: String,
        user: JSONObject
    ) {
        with(sharedPrefs.edit()) {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("user_id", user.getString("id"))
            putString("user_email", user.getString("email"))
            putString("user_full_name", user.getString("fullName"))
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun updateLoginStatus(isLoggedIn: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean("is_logged_in", isLoggedIn)
            apply()
        }
    }
}