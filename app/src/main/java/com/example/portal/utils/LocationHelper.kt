package com.example.portal.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.portal.models.LocationModel
import com.example.portal.api.UserServe
import com.example.portal.models.LocationUserModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
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

    fun handleFetchedLocations(googleMap: GoogleMap, locations: List<LocationUserModel>) {
        // Add markers based on fetched locations
        locations.forEach { location ->
            val latLng = LatLng(location.latitude.toDouble(), location.longitude.toDouble())
            val markerOptions = MarkerOptions().position(latLng).title(location.plateNumber)
            markerOptions.icon(
                BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_BLUE
                )
            )
            googleMap.addMarker(markerOptions)
        }
    }
}