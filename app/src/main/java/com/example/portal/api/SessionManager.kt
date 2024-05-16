package com.example.portal.api

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.portal.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object SessionManager {

    fun signOut(context: Context, navController: NavController) {
        val sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val accessToken = sharedPreferences.getString("accessToken", null)
        val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
            .create(UserServe::class.java)

        val call = retrofitService.logout("Bearer $accessToken")
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Clear shared preferences
                    editor.clear()
                    editor.apply()

                    // Show a toast message
                    Toast.makeText(context, "Logout successful", Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.LogInPage)

                } else {
                    Toast.makeText(context, "Logout failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Logout failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}