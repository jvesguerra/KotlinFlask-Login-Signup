package com.example.portal.api

import com.google.gson.annotations.SerializedName

data class GoogleSignInRequest(
    @SerializedName("idToken")
    val idToken: String
)
