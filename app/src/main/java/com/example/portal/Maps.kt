package com.example.portal

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.DelicateCoroutinesApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

private const val TAG = "MapsFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
class Maps : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener,
    OnMapReadyCallback {
    private var userId: Int = 0 // temporary delete later

    private lateinit var googleMap: GoogleMap
    private var lastKnownLocation: Location? = null
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)
    private val locationList: MutableList<LocationModel> = mutableListOf()

    private var foregroundOnlyLocationServiceBound = false
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var foregroundOnlyLocationButton: Button
    private lateinit var outputTextView: TextView
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            if (sharedPreferences != null) {
                updateButtonState(sharedPreferences.getBoolean(
                    SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
                )
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()
        sharedPreferences = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.maps, container, false)
        foregroundOnlyLocationButton = view.findViewById(R.id.foreground_only_location_button)
        outputTextView = view.findViewById(R.id.output_text_view)

        foregroundOnlyLocationButton.setOnClickListener {
            val enabled = sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                if (foregroundPermissionApproved()) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates() ?: Log.d(TAG, "Service Not Bound")
                } else {
                    requestForegroundPermissions()
                }
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        updateButtonState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val serviceIntent = Intent(requireContext(), ForegroundOnlyLocationService::class.java)
        requireActivity().bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        val lagunaCoordinates = LatLng(14.165104501414891, 121.24175774591879) // Coordinates for Laguna, Philippines
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lagunaCoordinates, 12.0f))

        // Fetch initial location data and add markers
        fetchLocations()
    }

    private fun fetchLocations() {
        retrofitService.getLocations().enqueue(object : Callback<List<LocationModel>> {
            override fun onResponse(call: Call<List<LocationModel>>, response: Response<List<LocationModel>>) {
                if (response.isSuccessful) {
                    locationList.addAll(response.body() ?: emptyList())

                    // Add markers based on fetched locations
                    if (locationList.isNotEmpty()) {
                        locationList.forEach { location ->
                            val latLng = LatLng(location.latitude.toDouble(), location.longitude.toDouble())
                            val markerOptions = MarkerOptions().position(latLng).title("Marker")
                            markerOptions.icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_BLUE
                                )
                            )
                            googleMap.addMarker(markerOptions)
                        }
                    }
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

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            requireActivity().unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }



    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                requireView(),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }


    // TODO: Step 1.0, Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()

                else -> {
                    // Permission denied.
                    updateButtonState(false)

                    Snackbar.make(
                        requireView(),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                requireContext().packageName,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }


    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            foregroundOnlyLocationButton.text = getString(R.string.stop_location_updates_button_text)
        } else {
            foregroundOnlyLocationButton.text = getString(R.string.start_location_updates_button_text)
        }
    }

    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs = "$output\n${outputTextView.text}"
        outputTextView.text = outputWithPreviousLogs
    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )
            lastKnownLocation = location

            val latLng = location?.toLatLng()

            if (latLng != null) {
                val latitude = latLng.first
                val longitude = latLng.second

                // Main gate entry point
                // put range
                val givenLatitude = 14.1676560638653
                val givenLongitude = 121.243494368555
                val rangeLatitude = 14.1676560638653..14.171871318554894  // Replace with your given latitude
                val rangeLongitude = 121.243494368555..121.26577861463531  // Replace with your given longitude

                if (latitude < givenLatitude || longitude < givenLongitude || latitude in rangeLatitude || longitude in rangeLongitude ) {
                    //showToast("INSIDE CAMPUS")
                }else {
                    //showToast("OUTSIDE CAMPUS")
                }

                // Now you can use latitude and longitude as needed
                logResultsToScreen("Latitude: $latitude, Longitude: $longitude")
            } else {
                // Handle case where location is unknown
                logResultsToScreen("Unknown location")
            }

            updateMapLocation()
        }
    }
//    private fun showToast(message: String) {
//        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
//    }

    private fun generateRandomNumericId(): Long {
        val timestamp = System.currentTimeMillis()
        val randomPart = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
        return timestamp + randomPart
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun updateMapLocation() {
        if (lastKnownLocation != null) {
            val latLng = lastKnownLocation!!.toLatLng()
            val lat = latLng.first
            val long = latLng.second
            // Update map with the latest location
            val location = LatLng(lat, long)
            googleMap.addMarker(MarkerOptions().position(location).title("Me"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12.0f))
            val userId = sharedPreferences.getInt("userId", 0)
            val randomNumericId = generateRandomNumericId()
            val timestamp = System.currentTimeMillis()
            val newLocation = LocationModel(
                locationId = 0,
                userId = userId,
                latitude=lat.toFloat(),
                longitude = long.toFloat(),
                timestamp = timestamp
            )

            retrofitService.addLocation(newLocation).enqueueVoid {
                println("New Location added successfully")
            }
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Maps().apply {
                arguments = Bundle().apply {
                }
            }
    }
}

//fun <T> Call<T>.enqueue(callback: (response: T?) -> Unit) {
//    this.enqueue(object : Callback<T> {
//        override fun onResponse(call: Call<T>, response: Response<T>) {
//            if (response.isSuccessful) {
//                callback.invoke(response.body())
//            } else {
//                println("Error: ${response.code()}")
//            }
//        }
//
//        override fun onFailure(call: Call<T>, t: Throwable) {
//            println("Network request failed: ${t.message}")
//        }
//    })
//}