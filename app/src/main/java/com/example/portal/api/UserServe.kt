package com.example.portal.api

import com.example.portal.GoogleSignInRequest
import com.example.portal.models.DriverSignUpRequest
import com.example.portal.models.UserModel
import com.example.portal.models.UserResponse
import com.example.portal.models.VehicleModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface UserServe {
    @POST("/google-sign-in-endpoint")
    fun signInWithGoogle(@Body request: GoogleSignInRequest): Call<String>

    @POST("/signin")
    fun signIn(@Body newUser: UserModel): Call<UserResponse>

    @POST("/register")
    fun register(@Body newUser: UserModel): Call<UserModel>

    @POST("/register_driver")
    fun registerDriver(@Body request: DriverSignUpRequest): Call<Void>
}