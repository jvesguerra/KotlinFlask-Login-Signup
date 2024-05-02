package com.example.portal.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.Adapter
import com.example.portal.api.UserServe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserDeletion(private val context: Context, private val adapter: Adapter) {
    private lateinit var sharedPreferences: SharedPreferences
    private var accessToken: String? = null

    fun deleteUser(retrofitService: UserServe, userId: Int, position: Int) {
        sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)

        val call = retrofitService.adminDeleteUser("Bearer $accessToken")
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    adapter.removeItemAt(position)
                    Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle error, could use response.code() to tailor the message
                    Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}