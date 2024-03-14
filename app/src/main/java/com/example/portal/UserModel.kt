package com.example.portal

import com.google.gson.annotations.SerializedName

class UserModel (
    @SerializedName("id")
    val userId: Int,

    @SerializedName("fullname")
    val fullname: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("userType")
    val userType: Int,

    @SerializedName("locationId")
    val locationId: Long,
)

data class UserResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: UserModel?
)