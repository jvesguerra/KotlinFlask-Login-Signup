package com.example.portal.models

import com.google.gson.annotations.SerializedName

class UserModel(
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

    @SerializedName("isQueued")
    val isQueued: Boolean,

    @SerializedName("isPetitioned")
    val isPetitioned: Int,

    )

data class UserResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: UserModel?,
)

data class MessageResponse(
    @SerializedName("message") val message: String?,
)

data class EditUserResponse(val message: String)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String
)

data class Credentials(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String)

data class Data(
    @SerializedName("logged_in_as") val logged_in_as: String,
    @SerializedName("data") val data: String)

data class DriverSignUpRequest(
    val user: UserModel,
    val vehicle: VehicleModel
)
