package com.example.portal.models

import com.google.gson.annotations.SerializedName

class VehicleModel (
    @SerializedName("vehicleId")
    val vehicleId: Int,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("plateNumber")
    val plateNumber: String,

    @SerializedName("route")
    val route: String,

    @SerializedName("isAvailable")
    val isAvailable: Boolean,

    @SerializedName("hasDeparted")
    val hasDeparted: Boolean,

    @SerializedName("isFull")
    val isFull: Boolean,

    @SerializedName("queuedUsers")
    val queuedUsers: Int,
)