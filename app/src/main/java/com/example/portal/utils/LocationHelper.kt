package com.example.portal.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.portal.models.LocationModel
import com.example.portal.api.UserServe
import com.example.portal.logResultsToScreen
import com.example.portal.models.LocationUserModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object LocationHelper {
    private lateinit var sharedPreferences: SharedPreferences
    private var accessToken: String? = null
    fun fetchLocations(context: Context, retrofitService: UserServe,
                       callback: (List<LocationUserModel>) -> Unit) {
        sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)
        retrofitService.getLocations("Bearer $accessToken").enqueue(object : Callback<List<LocationUserModel>> {
            override fun onResponse(call: Call<List<LocationUserModel>>, response: Response<List<LocationUserModel>>) {
                if (response.isSuccessful) {
                    val locationList = response.body() ?: emptyList()
                    Log.e("LOCATION", locationList.toString())
                    callback(locationList)
                } else {
                    println("Error: ${response.code()}")
                    println("Error Body: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<LocationUserModel>>, t: Throwable) {
                println("Network request failed: ${t.message}")
            }
        })
    }

    fun handleFetchedLocations(context: Context, googleMap: GoogleMap, locations: List<LocationUserModel>, outputTextView: TextView) {
        // Add markers based on fetched locations
        locations.forEach { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            val markerOptions = MarkerOptions().position(latLng).title(location.plateNumber)
            markerOptions.icon(
                BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_BLUE
                )
            )


            // Define the boundaries of the campus area
            val campusBounds = LatLngBounds(
                LatLng(14.152992744657807, 121.22340588366656), // Southwest corner
                LatLng(14.17011572642615, 121.25226645266653)  // Northeast corner
            )

            val isInsideCampus = campusBounds.contains(latLng)
            val message = if (isInsideCampus) "INSIDE CAMPUS" else "OUTSIDE CAMPUS"
            //showToast(context, message)
            //logResultsToScreen(message,outputTextView)
            if (isInsideCampus){
                googleMap.addMarker(markerOptions)
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}