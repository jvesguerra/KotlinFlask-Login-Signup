package com.example.portal.models

import com.google.gson.annotations.SerializedName

class DriverMod (
    @SerializedName("userId")
    val userId: Int? = null,

    @SerializedName("firstName")
    val firstName: String? = null,

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("contactNumber")
    val contactNumber: String? = null,

    @SerializedName("route")
    val route: String? = null,

    @SerializedName("plateNumber")
    val plateNumber: String? = null,
)