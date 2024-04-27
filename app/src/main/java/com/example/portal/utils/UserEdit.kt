package com.example.portal.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.portal.Adapter
import com.example.portal.api.UserServe
import com.example.portal.models.DriverVecLocModel
import com.example.portal.models.EditUserModel
import com.example.portal.models.EditUserResponse
import com.example.portal.models.MessageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserEdit(private val context: Context, private val adapter: Adapter) {
    fun editUser(retrofitService: UserServe, userId: Int, position: Int, userModel: EditUserModel) {
        val call = retrofitService.editUser(userId, userModel)
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