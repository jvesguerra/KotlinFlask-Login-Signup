package com.example.portal.api

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.portal.R

object SessionManager {

    fun signOut(context: Context) {
        val sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        println("User logged out")

        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        Toast.makeText(context, "IsLoggedIn: $isLoggedIn", Toast.LENGTH_SHORT).show()

    }
}