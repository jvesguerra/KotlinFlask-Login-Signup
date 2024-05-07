package com.example.portal.admin

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.Adapter
import com.example.portal.R
import com.example.portal.api.OnDeleteUserListener
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.DriverVecLocModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Users : Fragment(), OnDeleteUserListener {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Adapter
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private var accessToken: String? = null
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.users, container, false)
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        val logoutButton: Button = view.findViewById(R.id.btnLogout)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = Adapter(
            onDeleteUserListener = this@Users,
            onQueueUserListener = null,
            context = requireContext(),
            contextType = Adapter.ContextType.USERS,
            items = mutableListOf()
        )
        recyclerView.adapter = adapter

        logoutButton.setOnClickListener {
            signOut()
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            Toast.makeText(requireContext(), "IsLoggedIn: $isLoggedIn", Toast.LENGTH_SHORT).show()
            Navigation.findNavController(view).navigate(R.id.logout)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        scheduledExecutorService.scheduleAtFixedRate({
            getUsers()
        }, 0, 30, TimeUnit.SECONDS) // Fetch every 10 seconds, adjust as needed
    }

    override fun onPause() {
        super.onPause()
        scheduledExecutorService.shutdownNow() // Stop all currently executing tasks
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor() // Prepare for next onResume
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scheduledExecutorService.shutdown() // Ensure no memory leaks from the executor service
    }

    private fun getUsers() {
        val call = retrofitService.getUsers("Bearer $accessToken")
        call.enqueue(object : Callback<List<DriverVecLocModel>> {
            override fun onResponse(call: Call<List<DriverVecLocModel>>, response: Response<List<DriverVecLocModel>>) {
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    adapter.updateData(items) // Update the adapter's data
                }else {
                    Log.e("API_RESPONSE", "Unsuccessful response: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<List<DriverVecLocModel>>, t: Throwable) {
                Log.e("API_ERROR", "Error fetching data", t)
            }
        })
    }

    private fun signOut() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        println("User logged out")
    }
    override fun onDeleteUser(userId: Int, position: Int) {
        val call = retrofitService.adminDeleteUser("Bearer $accessToken") // Assuming retrofitService is your Retrofit instance's service
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    adapter.removeItemAt(position)
                    Toast.makeText(requireContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle error, could use response.code() to tailor the message
                    Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle failure
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}