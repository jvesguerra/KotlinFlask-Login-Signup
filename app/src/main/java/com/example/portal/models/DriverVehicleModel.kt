package com.example.portal.models

import com.google.gson.annotations.SerializedName

class DriverVehicleModel (
    @SerializedName("userId")
    val userId: Int,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("contactNumber")
    val contactNumber: String,

    @SerializedName("rating")
    val rating: Long,

    @SerializedName("userType")
    val userType: Int,

    @SerializedName("isActive")
    val isActive: Boolean,

    @SerializedName("authorized")
    val authorized: Boolean,

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