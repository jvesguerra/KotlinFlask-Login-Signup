package com.example.portal.driver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.example.portal.api.ForegroundOnlyLocationService
import com.example.portal.R
import com.example.portal.SharedPreferenceUtil
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.enqueueVoid
import com.example.portal.models.LocationModel
import com.example.portal.toLatLng
import com.example.portal.utils.PermissionHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

//private const val TAG = "DRIVER HOME 2"
//private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

/*
startLocationUpdates()
- Schedules location updates and fetches passenger and petition counts.

updateMapLocation()
- Updates the map with the latest location and sends the location data to the server via Retrofit

ForegroundOnlyBroadcastReceiver()
- Checks if vehicle is within campus
*/

class DriverHome2 : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    // Constants
    private val tag = "DRIVER HOME 2"
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

    // Retrofit Service
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)

    // SharedPreferences and other variables
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private lateinit var handler: Handler
    private var accessToken: String? = null

    // UI Elements
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver
    private lateinit var foregroundOnlyLocationButton: Button
    private lateinit var takePassengersButton: Button
    private lateinit var viewQueuedList: Button
    private lateinit var outputTextView: TextView
    private lateinit var petitionCountText: TextView
    private lateinit var incomingPassengersText: TextView

    // Location variables
    private var lastKnownLocation: Location? = null

    // Service connection
    private var foregroundOnlyLocationServiceBound = false
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        // MAPS
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
        // Initialize variables
        val view = inflater.inflate(R.layout.driver_home2, container, false)
        sharedPreferences =
            requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        accessToken = sharedPreferences.getString("accessToken", null)

        // Initialize UI Elements
        foregroundOnlyLocationButton = view.findViewById(R.id.foreground_only_location_button)
        takePassengersButton = view.findViewById(R.id.takePassengers)
        viewQueuedList = view.findViewById(R.id.queuedList)
        //outputTextView = view.findViewById(R.id.output_text_view)
        petitionCountText = view.findViewById(R.id.petitionCount)
        incomingPassengersText = view.findViewById(R.id.incomingPassengersText)

        // Button Listeners
        foregroundOnlyLocationButton.setOnClickListener {
            val enabled =
                sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                if (PermissionHelper.foregroundPermissionApproved(requireContext())) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates() ?: Log.d(
                        tag,
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

        takePassengersButton.setOnClickListener {
            takePassengers(view)
        }

        viewQueuedList.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toQueuedList)
        }

        // Hide button
        foregroundOnlyLocationButton.visibility = View.INVISIBLE

        return view
    }

    private fun takePassengers(view: View) {
        val call3 = retrofitService.takePassengers("Bearer $accessToken")
        call3.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("Success", response.code().toString())
                    Navigation.findNavController(view).navigate(R.id.toQueuedList)
                } else {
                    Log.d("Error", response.code().toString())
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPetitionCount(textView: TextView) {
        val call2 = retrofitService.getPetition("Bearer $accessToken")
        call2.enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.isSuccessful) {
                    val incomingPassengersCount: Int? = response.body()
                    incomingPassengersCount?.let {
                        handler.post {
                            val text = "Passengers in Petition: $it"
                            textView.text = text
                            Log.d("Success", response.code().toString())
                        }
                    }
                } else {
                    Log.d("Error", response.code().toString())
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPassengerCount(textView: TextView) {
        val call = retrofitService.getIncomingPassengers("Bearer $accessToken")
        call.enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.isSuccessful) {
                    val incomingPassengersCount: Int? = response.body()
                    incomingPassengersCount?.let {
                        // Update UI on the main thread
                        handler.post {
                            val text = "$it"
                            textView.text = text
                        }
                    }
                } else {
                    Log.d("Error", response.code().toString())
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

        Handler(Looper.getMainLooper()).postDelayed({
            foregroundOnlyLocationButton.performClick()
        }, 1000)

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutorService.scheduleAtFixedRate({
            // Execute the location updates request asynchronously
            GlobalScope.launch(Dispatchers.IO) {

                // Fetch passenger and petition counts asynchronously
                fetchPassengerCount(incomingPassengersText)
                fetchPetitionCount(petitionCountText)

//                // Introduce a delay before updating the map location
//                delay(5000) // Adjust the delay duration as needed
            }

        }, 0, 30, TimeUnit.SECONDS)

        scheduledExecutorService.scheduleAtFixedRate({
            // Execute the location updates request asynchronously
            GlobalScope.launch(Dispatchers.IO) {
                foregroundOnlyLocationService?.requestLocationUpdates()

                delay(3000)
                updateMapLocation()
            }

        }, 0, 5, TimeUnit.SECONDS)
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

        scheduledExecutorService.shutdown()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(tag, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(tag, "User interaction was cancelled.")

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

                updateMapLocation()

                // Main gate entry point
                // put range
//                val givenLatitude = 14.1676560638653
//                val givenLongitude = 121.243494368555
//                val rangeLatitude =
//                    14.1676560638653..14.171871318554894  // Replace with your given latitude
//                val rangeLongitude =
//                    121.243494368555..121.26577861463531  // Replace with your given longitude
//
//                if (latitude < givenLatitude || longitude < givenLongitude || latitude in rangeLatitude || longitude in rangeLongitude) {
//                    //showToast("INSIDE CAMPUS")
//                } else {
//                    //showToast("OUTSIDE CAMPUS")
//                }

                // Now you can use latitude and longitude as needed
                //logResultsToScreen("Latitude: $latitude, Longitude: $longitude", outputTextView)
            } else {
                // Handle case where location is unknown
                //logResultsToScreen("Unknown location", outputTextView)
            }

        }
    }

    private fun updateMapLocation() {
        lastKnownLocation?.let { location ->
            val latLng = location.toLatLng()
            val lat = latLng.first
            val long = latLng.second
            val timestamp = System.currentTimeMillis()
            val newLocation = LocationModel(
                locationId = 0,
                userId = 0,
                latitude = lat,
                longitude = long,
                timestamp = timestamp
            )

            retrofitService.addLocation("Bearer $accessToken", newLocation).enqueueVoid {
                println("New Location added successfully")
            }
        } ?: run {
            Log.e(tag, "Last known location is null")
        }
    }
}