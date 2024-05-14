package com.example.portal

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.portal.api.ForegroundOnlyLocationService
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.LocationModel
import com.example.portal.utils.LocationHelper
import com.example.portal.utils.PermissionHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val TAG = "MapsFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
class Maps : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private var lastKnownLocation: Location? = null
    private val retrofitService: UserServe =
        RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)
    private val locationList: MutableList<LocationModel> = mutableListOf()

    private var foregroundOnlyLocationServiceBound = false
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var foregroundOnlyLocationButton: Button
    private lateinit var outputTextView: TextView

    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private lateinit var handler: Handler

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
                updateButtonState(
                    sharedPreferences.getBoolean(
                        SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()
        sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.maps, container, false)
        foregroundOnlyLocationButton = view.findViewById(R.id.foreground_only_location_button)
        outputTextView = view.findViewById(R.id.output_text_view)

        foregroundOnlyLocationButton.setOnClickListener {
            val enabled =
                sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                if (PermissionHelper.foregroundPermissionApproved(requireContext())) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates() ?: Log.d(
                        TAG,
                        "Service Not Bound"
                    )
                } else {
                    PermissionHelper.requestForegroundPermissions(
                        requireActivity(),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        }

        foregroundOnlyLocationButton.visibility = View.INVISIBLE
        return view
    }

    override fun onStart() {
        super.onStart()
        updateButtonState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val serviceIntent = Intent(requireContext(), ForegroundOnlyLocationService::class.java)
        requireActivity().bindService(
            serviceIntent,
            foregroundOnlyServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Handler(Looper.getMainLooper()).postDelayed({
            foregroundOnlyLocationButton.performClick()
        }, 3000)
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        val lagunaCoordinates =
            LatLng(14.165104501414891, 121.24175774591879) // Coordinates for Laguna, Philippines
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lagunaCoordinates, 12.0f))

        startFetchingLocations()
    }

    private fun startFetchingLocations() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutorService.scheduleAtFixedRate({
            foregroundOnlyLocationService?.requestLocationUpdates()
            fetchLocations()
        }, 0, 6, TimeUnit.SECONDS) // Fetch every 10 seconds, adjust as needed
    }

    private fun fetchLocations() {
        LocationHelper.fetchLocations(requireContext(), retrofitService) { fetchedLocations ->
            LocationHelper.handleFetchedLocations(
                requireContext(),
                googleMap,
                fetchedLocations,
                outputTextView
            )
        }
    }


    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
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

        // Shutdown the ScheduledExecutorService if it's not already shutdown
        if (!scheduledExecutorService.isShutdown) {
            try {
                scheduledExecutorService.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down ScheduledExecutorService: ${e.message}")
            }
        }
    }

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
            foregroundOnlyLocationButton.text =
                getString(R.string.stop_location_updates_button_text)
        } else {
            foregroundOnlyLocationButton.text =
                getString(R.string.start_location_updates_button_text)
        }
    }

    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )

//            Log.e("MAPS", "ForegroundOnlyBroadcastReceiver")
//            lastKnownLocation = location
//
//            val latLng = location?.toLatLng()
//
//            if (latLng != null) {
//                val latitude = latLng.first
//                val longitude = latLng.second
//
//                // Define the boundaries of the campus area
//                val campusBounds = LatLngBounds(
//                    LatLng(14.1676560638653, 121.243494368555), // Southwest corner
//                    LatLng(14.171871318554894, 121.26577861463531) // Northeast corner
//                )
//
//                val currentLatLng = LatLng(latitude, longitude)
//
//                // Check if the current location is inside the campus bounds
//                val isInsideCampus = campusBounds.contains(currentLatLng)
//
//                val message = if (isInsideCampus) "INSIDE CAMPUS" else "OUTSIDE CAMPUS"
//
//                // Update text view only if it's initialized
//                outputTextView?.let {
//                    it.text = "Latitude: $latitude, Longitude: $longitude\n$message"
//                }
//
//                Log.e("MAPS", message)
//                // Log the location results to screen
//                logResultsToScreen(
//                    "Latitude: $latitude, Longitude: $longitude\n$message",
//                    outputTextView
//                )
//            } else {
//                // Handle case where location is unknown
//                logResultsToScreen("Unknown location", outputTextView)
//            }

            // Update map location if needed
            //updateMapLocation()outputTextView
        }
    }
}

//@OptIn(DelicateCoroutinesApi::class)
//private fun updateMapLocation() {
//    if (lastKnownLocation != null) {
//        val latLng = lastKnownLocation!!.toLatLng()
//        val lat = latLng.first
//        val long = latLng.second
//        // Update map with the latest location
//        val location = LatLng(lat, long)
//        googleMap.addMarker(MarkerOptions().position(location).title("Me"))
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12.0f))
//        val userId = sharedPreferences.getInt("userId", 0)
//        val timestamp = System.currentTimeMillis()
//        val checkId = "USER ID: $userId"
//        showToast(checkId)
//        val newLocation = LocationModel(
//            locationId = 0,
//            userId = userId,
//            latitude=lat.toFloat(),
//            longitude = long.toFloat(),
//            timestamp = timestamp
//        )
//
//        retrofitService.addLocation(newLocation).enqueueVoid {
//            println("New Location added successfully")
//        }
//    }
//}