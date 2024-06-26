package com.example.portal.models

import com.google.gson.annotations.SerializedName

class EditUserModel (
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
)
