package com.example.portal

import com.google.gson.annotations.SerializedName
import java.util.Date
class LocationModel (
    @SerializedName("locationId")
    val locationId: Long,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("latitude")
    val latitude: Float,

    @SerializedName("longitude")
    val longitude: Float,

    @SerializedName("timestamp")
    val timestamp: Long
)