package com.example.portal

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface UserServe {
    @POST("/google-sign-in-endpoint")
    fun signInWithGoogle(@Body request: GoogleSignInRequest): Call<String>

    @POST("/signin")
    fun signIn(@Body newUser: UserModel): Call<UserResponse>

    @POST("/register")
    fun register(@Body newUser: UserModel): Call<Void>
}