package com.example.portal.admin

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.portal.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.AdminAdapter
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.DriverVehicle
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AdminHome : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminAdapter

    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.admin_home, container, false)
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        val logoutButton: Button = view.findViewById(R.id.btnLogout)
        logoutButton.setOnClickListener {
            signOut()

            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            Toast.makeText(requireContext(), "IsLoggedIn: $isLoggedIn", Toast.LENGTH_SHORT).show()
            Navigation.findNavController(view).navigate(R.id.logout)
        }
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdminAdapter(this, mutableListOf())
        recyclerView.adapter = adapter

        val call = retrofitService.getDriverVehicles()

        call.enqueue(object : Callback<List<DriverVehicle>> {
            override fun onResponse(call: Call<List<DriverVehicle>>, response: Response<List<DriverVehicle>>) {
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    adapter.updateData(items) // Update the adapter's data
                }
            }

            override fun onFailure(call: Call<List<DriverVehicle>>, t: Throwable) {
                Log.e("AdminHome", "Error fetching data", t)
            }
        })

        return view
    }
    private fun getSampleItems(): List<String> {
        // Replace this with your actual data retrieval logic
        return listOf("Item 1", "Item 2", "Item 3")
    }

    fun deleteItem(userId: Int, position: Int) {
        val call = retrofitService.adminDeleteUser(userId) // Assuming retrofitService is your Retrofit instance's service
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

    private fun signOut() {
         val editor = sharedPreferences.edit()
         editor.clear()
         editor.apply()

        // Add any additional logic for sign-out, such as navigating to a login screen
        // For simplicity, let's just print a log message
        println("User logged out")
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AdminHome().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

private fun <T> Response<T>.enqueue(callback: Callback<T>) {

}
