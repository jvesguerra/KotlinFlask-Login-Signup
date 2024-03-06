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

    @SerializedName("type")
    val type: Int,

    @SerializedName("locationId")
    val locationId: Long,
)