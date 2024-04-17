package com.example.portal.utils

import com.example.portal.LocationModel
import com.example.portal.api.UserServe
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object LocationHelper {
    fun fetchLocations(retrofitService: UserServe,
                       callback: (List<LocationModel>) -> Unit) {
        retrofitService.getLocations().enqueue(object : Callback<List<LocationModel>> {
            override fun onResponse(call: Call<List<LocationModel>>, response: Response<List<LocationModel>>) {
                if (response.isSuccessful) {
                    val locationList = response.body() ?: emptyList()
                    callback(locationList)
                } else {
                    println("Error: ${response.code()}")
                    println("Error Body: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<LocationModel>>, t: Throwable) {
                println("Network request failed: ${t.message}")
            }
        })
    }

    fun handleFetchedLocations(googleMap: GoogleMap, locations: List<LocationModel>) {
        // Add markers based on fetched locations
        locations.forEach { location ->
            val latLng = LatLng(location.latitude.toDouble(), location.longitude.toDouble())
            val markerOptions = MarkerOptions().position(latLng).title(location.userId.toString())
            markerOptions.icon(
                BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_BLUE
                )
            )
            googleMap.addMarker(markerOptions)
        }
    }
}