package com.example.portal.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.portal.Adapter
import com.example.portal.api.UserServe
import com.example.portal.models.EditDriverModel
import com.example.portal.models.EditUserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserEdit(private val context: Context, private val adapter: Adapter) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
    private var accessToken: String? = sharedPreferences.getString("accessToken", null)
    fun editUser(retrofitService: UserServe, userId: Int, position: Int, userModel: EditDriverModel) {
        val call = retrofitService.editUser("Bearer $accessToken", userId, userModel)
        call.enqueue(object : Callback<EditUserResponse> {
            override fun onResponse(call: Call<EditUserResponse>, response: Response<EditUserResponse>) {
                if (response.isSuccessful) {
                    // Update local data if needed
                    // For example, find the corresponding DriverVehicle object in items list and update its authorized value
                } else {
                    // Handle API error
                    val errorMessage = response.message() // Get the error message from the response
                    // Handle the error message appropriately, such as displaying it to the user or logging it
                    Log.e("API Error", "Error message: $errorMessage")
                }
            }

            override fun onFailure(call: Call<EditUserResponse>, t: Throwable) {
                // Handle network error
                // You can log the error message or display it to the user
                Log.e("Network Error", "Error: ${t.message}", t)
            }
        })
    }
}