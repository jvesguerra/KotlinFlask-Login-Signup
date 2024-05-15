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
import android.widget.TextView
import android.widget.Toast
import com.example.portal.R
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.DriverMod
import com.example.portal.models.DriverVecLocModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class UserHome3 : Fragment() {
    private var driverID: String? = null
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private var accessToken: String? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var itemNameTextView: TextView
    private lateinit var routeTextView: TextView
    private lateinit var plateNumberTextView: TextView
    private lateinit var emailTextView: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.user_home3, container, false)

        itemNameTextView = view.findViewById(R.id.itemNameTextView)
        routeTextView = view.findViewById(R.id.RouteTextView)
        plateNumberTextView = view.findViewById(R.id.PlateNumberTextView)
        emailTextView = view.findViewById(R.id.EmailTextView)

        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)
        driverID = arguments?.getInt("driverID").toString()

        fetchedDriver(driverID!!.toInt())
        return view
    }

    private fun fetchedDriver(driverID: Int?) {
        driverID?.let { retrofitService.getDriver("Bearer $accessToken", it) }
            ?.enqueue(object : Callback<DriverMod> {
                override fun onResponse(
                    call: Call<DriverMod>,
                    response: Response<DriverMod>
                ) {
                    val rawResponse = response.raw().toString() // Get raw response string
                    Log.d("RawResponse", rawResponse) // Log raw response
                    if (response.isSuccessful) {
                        val userData = response.body()
                        if (userData != null) {
                            val fullname = "${userData.firstName} ${userData.lastName}"
                            itemNameTextView.text = fullname
                            routeTextView.text = userData.route.toString()
                            plateNumberTextView.text = userData.plateNumber.toString()
                            emailTextView.text = userData.email.toString()
                        }

                    } else {
                        // Handle error, could use response.code() to tailor the message
                        Toast.makeText(context, "Failed to fetch driver", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DriverMod>, t: Throwable) {
                    // Handle failure
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }
}