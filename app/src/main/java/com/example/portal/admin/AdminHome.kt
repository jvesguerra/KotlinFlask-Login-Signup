package com.example.portal.admin

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.portal.Adapter
import com.example.portal.api.OnDeleteUserListener
import com.example.portal.api.OnQueueUserListener
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.SessionManager
import com.example.portal.api.UserServe
import com.example.portal.models.DriverVecLocModel
import com.example.portal.models.EditDriverModel
import com.example.portal.utils.UserEdit
import com.example.portal.utils.UserQueue
import com.example.portal.utils.UserRemoveQueue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AdminHome : Fragment(), OnDeleteUserListener, OnQueueUserListener {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Adapter
    private var accessToken: String? = null
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private lateinit var handler: Handler
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.admin_home, container, false)
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)
        handler = Handler(Looper.getMainLooper())
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        val logoutButton: Button = view.findViewById(R.id.btnLogout)
        val mapsButton: Button = view.findViewById(R.id.btnMap)
        val pendingListsButton: Button = view.findViewById(R.id.btnPending)
        val usersButton: Button = view.findViewById(R.id.btnUsers)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = Adapter(
            onDeleteUserListener = this@AdminHome,
            onQueueUserListener = null,
            context = requireContext(),
            contextType = Adapter.ContextType.ADMIN_HOME,
            items = mutableListOf()
        )
        recyclerView.adapter = adapter


        // Button Listeners
        pendingListsButton.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toPendingLists)
        }

        usersButton.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toUsers)
        }

        mapsButton.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toMaps)
        }

        logoutButton.setOnClickListener {
            signOut()
            Navigation.findNavController(view).navigate(R.id.logout)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        scheduledExecutorService.scheduleAtFixedRate({
            fetchAuthenticatedDrivers()
        }, 0, 30, TimeUnit.SECONDS) // Adjust timing as needed
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

    private fun fetchAuthenticatedDrivers() {
        val call2 = retrofitService.getAuthDrivers("Bearer $accessToken")
        call2.enqueue(object : Callback<List<DriverVecLocModel>> {
            override fun onResponse(call: Call<List<DriverVecLocModel>>, response: Response<List<DriverVecLocModel>>) {
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    adapter.updateData(items) // Update the adapter's data

                    for (item in items) {
                        Log.d("UserDetails", "USER TYPE: ${item.userType}")
                        // Add more attributes as needed
                    }
                }
            }

            override fun onFailure(call: Call<List<DriverVecLocModel>>, t: Throwable) {
                Log.e("AdminHome", "Error fetching data", t)
            }
        })
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

    override fun onQueueUser(userId: Int, position: Int, vehicleId: Int) {
        val userQueue = UserQueue(requireContext(), adapter)
        userQueue.addQueuedUser(retrofitService, userId, position, vehicleId)
    }

    override fun onRemoveUserQueue(userId: Int, position: Int, vehicleId: Int) {
        val userRemoveQueue = UserRemoveQueue(requireContext(), adapter)
        userRemoveQueue.removeQueuedUser(retrofitService, userId, position, vehicleId)
    }

    override fun editUser(userId: Int, position: Int, userModel: EditDriverModel) {
        val editUser = UserEdit(requireContext(), adapter)
        editUser.editUser(retrofitService, userId, position, userModel)
    }

    private fun signOut() {
        SessionManager.signOut(requireContext())
    }
}

private fun <T> Response<T>.enqueue(callback: Callback<T>) {

}
