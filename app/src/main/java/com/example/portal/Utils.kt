package com.example.portal

import android.content.Context
import android.location.Location
import android.widget.TextView
import androidx.core.content.edit

/**
 * Returns the `location` object as a human readable string.
 */
//fun Location?.toText(): String {
//    return if (this != null) {
//        "($latitude, $longitude)"
//    } else {
//        "Unknown location"
//    }
//}
fun Location?.toLatLng(): Pair<Double, Double> {
    return if (this != null) {
        Pair(latitude, longitude)
    } else {
        Pair(0.0, 0.0) // Default values if location is unknown
    }
}


fun logResultsToScreen(output: String, outputTextView: TextView) {
    val outputWithPreviousLogs = "$output\n${outputTextView.text}"
    outputTextView.text = outputWithPreviousLogs
}

/**
 * Provides access to SharedPreferences for location to Activities and Services.
 */
internal object SharedPreferenceUtil {

    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The [Context].
     */
    fun getLocationTrackingPref(context: Context): Boolean =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            .getBoolean(KEY_FOREGROUND_ENABLED, false)

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun saveLocationTrackingPref(context: Context, requestingLocationUpdates: Boolean) =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE).edit {
            putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates)
        }
}
