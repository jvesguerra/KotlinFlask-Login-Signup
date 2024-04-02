package com.example.portal.models

import com.google.gson.annotations.SerializedName

class UserModel (
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

    @SerializedName("authorized")
    val authorized: Boolean,
)

data class UserResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: UserModel?
)

data class DriverSignUpRequest(
    val user: UserModel,
    val vehicle: VehicleModel
)