package com.example.portal.models

import com.google.gson.annotations.SerializedName
import java.util.Date
class LocationModel (
    @SerializedName("locationId")
    val locationId: Long,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("timestamp")
    val timestamp: Long
)