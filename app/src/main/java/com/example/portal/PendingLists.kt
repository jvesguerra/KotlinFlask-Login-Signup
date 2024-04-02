package com.example.portal

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.DriverVehicle
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class PendingLists : Fragment() {
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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.pending_lists, container, false)

        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        val logoutButton: Button = view.findViewById(R.id.btnLogout)
        val pendingListsButton: Button = view.findViewById(R.id.btnPending)
        pendingListsButton.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toPendingLists)
        }
        logoutButton.setOnClickListener {
            signOut()

            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            Toast.makeText(requireContext(), "IsLoggedIn: $isLoggedIn", Toast.LENGTH_SHORT).show()
            Navigation.findNavController(view).navigate(R.id.logout)
        }
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdminAdapter(requireContext(), mutableListOf())
        recyclerView.adapter = adapter

        val call = retrofitService.getPendingDrivers()

        call.enqueue(object : Callback<List<DriverVehicle>> {
            override fun onResponse(call: Call<List<DriverVehicle>>, response: Response<List<DriverVehicle>>) {
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    adapter.updateData(items) // Update the adapter's data
                }else {
                    Log.e("API_RESPONSE", "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<DriverVehicle>>, t: Throwable) {
                Log.e("API_ERROR", "Error fetching data", t)
            }
        })



        return view
    }
    private fun signOut() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Add any additional logic for sign-out, such as navigating to a login screen
        // For simplicity, let's just print a log message
        println("User logged out")
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
    companion object {
        fun newInstance(param1: String, param2: String) =
            PendingLists().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}