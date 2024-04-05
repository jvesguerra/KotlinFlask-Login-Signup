package com.example.portal

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DriverHome2 : Fragment() {
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        // Initialize handler on the main thread
        handler = Handler(Looper.getMainLooper())
        // Initialize scheduled executor service
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.driver_home2, container, false)
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        val userId = sharedPreferences.getInt("userId", 0)

        var incomingPassengersText: TextView = view.findViewById(R.id.incomingPassengersText)

        scheduledExecutorService.scheduleAtFixedRate({
            fetchPassengerCount(userId, incomingPassengersText)
        }, 0, 10, TimeUnit.SECONDS) // Fetch every 10 seconds, you can adjust this interval as needed

        return view
    }

    private fun fetchPassengerCount(userId: Int, textView: TextView) {
        val call = retrofitService.getIncomingPassengers(userId)
        call.enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.isSuccessful) {
                    val incomingPassengersCount: Int? = response.body()
                    incomingPassengersCount?.let {
                        // Update UI on the main thread
                        handler.post {
                            val text = "Incoming Passengers: $it"
                            textView.text = text
                            Log.d("PassengerCount", "Passenger count: $it")
                        }
                    }
                } else {
                    // Handle error, could use response.code() to tailor the message
                    Log.d("DriverHome2 Error", "Pain")
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                // Handle failure
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DriverHome2().apply {
                arguments = Bundle().apply {
                }
            }
    }
}