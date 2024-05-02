package com.example.portal.driver

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.portal.R
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverHome : Fragment() {
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.driver_home, container, false)
        sharedPreferences =
            requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("accessToken", "")
        val firstName = sharedPreferences.getString("firstName", "")
        val homeString: TextView = view.findViewById(R.id.driverHomeText)
        val readyButton: Button = view.findViewById(R.id.readyButton)

        homeString.text = "Hello $firstName,"
        readyButton.setOnClickListener {
            readyDriver(accessToken)
        }
        return view
    }

    private fun readyDriver(accessToken: String?) {
        val call =
            retrofitService.readyDriver("Bearer $accessToken")
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Driver set to ready successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    view?.let { Navigation.findNavController(it).navigate(R.id.toDriverHome2) }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to set driver ready",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
}