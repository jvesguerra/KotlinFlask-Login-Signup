package com.example.portal.models

import com.google.gson.annotations.SerializedName

class DriverVecLocModel (
        @SerializedName("userId")
        val userId: Int,

        @SerializedName("vehicleId")
        val vehicleId: Int,

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

        @SerializedName("isQueued")
        val isQueued: Boolean,

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
        val queuedUsers: List<String>,

        @SerializedName("latitude")
        val latitude: Double,

        @SerializedName("longitude")
        val longitude: Double,
    )