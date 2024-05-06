package com.example.portal.models

import com.google.gson.annotations.SerializedName

class LocationUserModel (
    @SerializedName("locationId")
    val locationId: Long,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("plateNumber")
    val plateNumber: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("timestamp")
    val timestamp: Long
)