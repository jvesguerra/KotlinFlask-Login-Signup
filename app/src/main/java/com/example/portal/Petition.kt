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
import com.example.portal.models.UserResponse
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
        val view = inflater.inflate(R.layout.petition, container, false)
        val route = arguments?.getString("route")
        var isPetitioned: Int? = null
        var petitionItemCount: Int = 0
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        petitionTitle = view.findViewById(R.id.petition)
        petitionCount = view.findViewById(R.id.petition_count)
        btnSignPetition = view.findViewById(R.id.signPetition)
        petitionTitle.text = route

        val accessToken = sharedPreferences.getString("accessToken", null);

        val dataCall = retrofitService.fetchData("Bearer $accessToken")
        dataCall.enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    val user = userResponse?.user
                    isPetitioned = user?.isPetitioned
                    // Check if isPetitioned is either 1 or 2
                    if (isPetitioned == 1 || isPetitioned == 2) {
                        btnSignPetition.text = "Cancel"

                    }
                }
            }
            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
            }
        })

        // PETITION COUNT
        var call2: Call<Int> = retrofitService.getForestryPetition("Bearer $accessToken")
        if(isPetitioned==2){
            call2 = retrofitService.getRuralPetition("Bearer $accessToken")
        }
        call2.enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.isSuccessful) {
                    val count = response.body()
                    if (count != null) {
                        // Use the count value
                        petitionCount.text = count.toString()
                        val petitionCountText = "$count people in petition"
                        petitionCount.text = petitionCountText
                    } else {
                        // Handle null response
                        Log.d("Response", "Response body is null")
                    }
                } else {
                    // Handle unsuccessful response
                    Log.d("Response", "Unsuccessful response: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                // Handle failure
                Log.e("Error", "Failed to fetch forestry petition count: ${t.message}", t)
            }
        })

        btnSignPetition.setOnClickListener {
            Log.d("AccessToken2", accessToken ?: "AccessToken is null") // Log the accessToken
            val call: Call<Void> = if (isPetitioned == 0) {
                retrofitService.addForestryPetition("Bearer $accessToken")
            }else{
                retrofitService.deleteForestryPetition("Bearer $accessToken")
            }

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
            val bundle = Bundle()
            bundle.putString("route", route)
            Navigation.findNavController(view).navigate(R.id.petitionReload, bundle)
        }

        return view
    }
}