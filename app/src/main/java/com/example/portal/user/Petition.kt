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
import com.example.portal.models.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Petition : Fragment() {
    private lateinit var petitionTitle: TextView
    private lateinit var petitionCount: TextView
    private lateinit var btnSignPetition: Button
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)
    private lateinit var sharedPreferences: SharedPreferences
    private var isPetitioned: Int? = null
    private var accessToken: String? = null
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private var route: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.petition, container, false)
        route = arguments?.getString("route")
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        petitionTitle = view.findViewById(R.id.petition)
        petitionCount = view.findViewById(R.id.petition_count)
        btnSignPetition = view.findViewById(R.id.signPetition)
        petitionTitle.text = route
        accessToken = sharedPreferences.getString("accessToken", null)
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        //route?.let { showToast(requireContext(), it) }

        setupButtonListener(route)

        return view
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        scheduledExecutorService.scheduleAtFixedRate({
            fetchUserDetails()
            fetchPetitionCount(route)
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

    private fun fetchUserDetails() {
        val call = retrofitService.fetchData("Bearer $accessToken")
        call.enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    isPetitioned = response.body()?.user?.isPetitioned
                    updateButtonText()
                } else {
                    Log.e("PetitionFragment", "Error fetching user details: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("PetitionFragment", "Failed to fetch user details", t)
            }
        })
    }

    private fun updateButtonText() {
        isPetitioned?.let {
            if (it == 1 || it == 2) {
                btnSignPetition.text = "Cancel Petition"
            } else {
                btnSignPetition.text = "Sign Petition"
            }
        }
    }

    private fun fetchPetitionCount(route: String?) {
//        if (isPetitioned == null || accessToken == null || route == null) {
//            Toast.makeText(context, "Missing data, cannot proceed.", Toast.LENGTH_SHORT).show()
//            return
//        }

        val call = when (route) {
            "Forestry" -> retrofitService.getForestryPetition("Bearer $accessToken")
            else -> {retrofitService.getRuralPetition("Bearer $accessToken")}
        }
        call.enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        petitionCount.text = "$it people in petition"
                    }
                } else {
                    Log.d("PetitionFragment", "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Log.e("PetitionFragment", "Failed to fetch petition count", t)
            }
        })
    }

    private fun setupButtonListener(route: String?) {
        btnSignPetition.setOnClickListener {
            if (isPetitioned == null || accessToken == null || route == null) {
                Toast.makeText(context, "Missing data, cannot proceed.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val call = if (isPetitioned == 0) {
                if (route == "Forestry") retrofitService.addForestryPetition("Bearer $accessToken")
                else retrofitService.addRuralPetition("Bearer $accessToken")
            } else {
                if (route == "Forestry") retrofitService.deletePetition("Bearer $accessToken")
                else retrofitService.deletePetition("Bearer $accessToken")
            }
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Petition update successful", Toast.LENGTH_SHORT).show()
                        fetchPetitionCount(route)
                        isPetitioned = 1 - (isPetitioned ?: 0) // Toggle isPetitioned status
                        updateButtonText()
                    } else {
                        Toast.makeText(context, "Failed to update petition", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}