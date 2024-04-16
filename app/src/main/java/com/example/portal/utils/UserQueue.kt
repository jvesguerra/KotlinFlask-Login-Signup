package com.example.portal.utils

import android.content.Context
import android.widget.Toast
import com.example.portal.Adapter
import com.example.portal.api.UserServe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserQueue(private val context: Context, private val adapter: Adapter) {
    fun addQueuedUser(retrofitService: UserServe, userId: Int, position: Int, vehicleId: Int) {
        val call = retrofitService.addQueuedUser(vehicleId,userId)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    //adapter.removeItemAt(position)
                    Toast.makeText(context, "Queued successfully", Toast.LENGTH_SHORT).show()
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