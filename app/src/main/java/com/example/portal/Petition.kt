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
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Petition : Fragment() {
    private lateinit var petitionTitle: TextView
    private lateinit var petitionCount: TextView
    private lateinit var btnSignPetition: Button
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.petition, container, false)
        val route = arguments?.getString("route")
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        petitionTitle = view.findViewById(R.id.petition)
        petitionCount = view.findViewById(R.id.petition_count)
        btnSignPetition = view.findViewById(R.id.signPetition)
        petitionTitle.text = route

        btnSignPetition.setOnClickListener {
            val accessToken = sharedPreferences.getString("accessToken", null);
            Log.d("AccessToken2", accessToken ?: "AccessToken is null") // Log the accessToken
            val call = retrofitService.addForestryPetition("Bearer $accessToken")
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        //adapter.removeItemAt(position)
                        Toast.makeText(context, "Petitioned successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        // Handle error, could use response.code() to tailor the message
                        Toast.makeText(context, "Failed to petition", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle failure
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        return view
    }
}