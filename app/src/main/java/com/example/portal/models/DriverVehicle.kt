package com.example.portal.models

import com.google.gson.annotations.SerializedName

class DriverVehicle (
    @SerializedName("userId")
    val userId: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("contactNumber")
    val contactNumber: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("rating")
    val rating: Long,

    @SerializedName("userType")
    val userType: Int,

    @SerializedName("isActive")
    val isActive: Boolean,

    @SerializedName("plateNumber")
    val plateNumber: String,

    @SerializedName("route")
    val route: String,
)