package com.example.portal.models

import com.google.gson.annotations.SerializedName

class EditDriverModel (
    @SerializedName("firstName")
    val firstName: String? = null, // Nullable

    @SerializedName("lastName")
    val lastName: String? = null, // Nullable

    @SerializedName("email")
    val email: String? = null, // Nullable

    @SerializedName("contactNumber")
    val contactNumber: String? = null, // Nullable

    @SerializedName("password")
    val password: String? = null, // Nullable

    @SerializedName("plateNumber")
    val plateNumber: String? = null, // Nullable

    @SerializedName("route")
    val route: String? = null, // Nullable
)
