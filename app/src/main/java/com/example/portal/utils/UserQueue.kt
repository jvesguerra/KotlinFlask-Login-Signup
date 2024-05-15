package com.example.portal.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.portal.Adapter
import com.example.portal.R
import com.example.portal.api.UserServe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserQueue(private val context: Context, private val adapter: Adapter) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
    private var accessToken: String? = sharedPreferences.getString("accessToken", null)
    fun addQueuedUser(retrofitService: UserServe, position: Int, vehicleId: Int, view: View) {
        val call = retrofitService.addQueuedUser("Bearer $accessToken",vehicleId)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    //adapter.removeItemAt(position)
                    //Toast.makeText(context, "Queued successfully", Toast.LENGTH_SHORT).show()

                    val bundle = Bundle().apply {
                        putInt("driverID", vehicleId)
                    }

                    Navigation.findNavController(view).navigate(R.id.toUserHome3, bundle)
                } else {
                    // Handle error, could use response.code() to tailor the message
                    Toast.makeText(context, "Failed to queue", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}