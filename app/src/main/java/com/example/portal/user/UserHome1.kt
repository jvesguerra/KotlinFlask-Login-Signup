package com.example.portal.user

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
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.UserQueueModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.properties.Delegates

class UserHome1 : Fragment() {
    private lateinit var view: View
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private var accessToken: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnForestry: Button
    private lateinit var btnRural: Button
    private var isQueued: Boolean = false // Initialize with a default value
    private var vehicleId: Int = 0 // Initialize with a default value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.user_home1, container, false)
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)
        btnForestry = view.findViewById(R.id.btnForestry)
        btnRural = view.findViewById(R.id.btnRural)
        val bundle = Bundle()

        // Disable buttons initially
        btnForestry.isEnabled = false
        btnRural.isEnabled = false

        // Check the queue status when the view is created
        checkIsQueued()

        btnForestry.setOnClickListener {
            bundle.putString("route", "Forestry") // Add "forestry" to the bundle
            Log.d("UserHome", vehicleId.toString())
            bundle.putInt("driverID", vehicleId)
            if (isQueued) {
                Navigation.findNavController(view).navigate(R.id.toUserHome3, bundle)
            } else {
                Navigation.findNavController(view).navigate(R.id.toAvailableVehicles, bundle)
            }
        }

        btnRural.setOnClickListener {
            bundle.putString("route", "Rural")
            bundle.putInt("driverID", vehicleId)
            if (isQueued) {
                Navigation.findNavController(view).navigate(R.id.toUserHome3, bundle)
            } else {
                Navigation.findNavController(view).navigate(R.id.toAvailableVehicles, bundle)
            }
        }

        return view
    }

    private fun checkIsQueued() {
        val call = retrofitService.getIsQueued("Bearer $accessToken")
        call.enqueue(object : Callback<UserQueueModel> {
            override fun onResponse(call: Call<UserQueueModel>, response: Response<UserQueueModel>) {
                if (response.isSuccessful) {
                    val queueData = response.body()
                    if (queueData != null) {
                        vehicleId = queueData.queuedDriver
                        isQueued = queueData.isQueued

                        //Log.d("UserHome2", vehicleId.toString())

                        // Enable buttons after receiving response
                        btnForestry.isEnabled = true
                        btnRural.isEnabled = true
                    }
                } else {
                    // Handle the case where the server response is not successful
                    Toast.makeText(context, "Failed to check queue status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserQueueModel>, t: Throwable) {
                // Handle the failure of the network request
                Toast.makeText(context, "Network request failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
