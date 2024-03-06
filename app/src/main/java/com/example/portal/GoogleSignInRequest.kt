package com.example.portal

import com.google.gson.annotations.SerializedName

data class GoogleSignInRequest(
    @SerializedName("idToken")
    val idToken: String
)
