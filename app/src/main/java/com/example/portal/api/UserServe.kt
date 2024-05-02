package com.example.portal.api

import com.example.portal.models.Credentials
import com.example.portal.models.DriverSignUpRequest
import com.example.portal.models.DriverVecLocModel
import com.example.portal.models.EditUserModel
import com.example.portal.models.EditUserResponse
import com.example.portal.models.LocationModel
import com.example.portal.models.LoginResponse
import com.example.portal.models.UserModel
import com.example.portal.models.UserResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserServe {
    // DELETE
    @DELETE("/admin_delete_user")
    fun adminDeleteUser(@Header("Authorization") token: String): Call<Void>

    @DELETE("/delete_petition")
    fun deletePetition(@Header("Authorization") token: String): Call<Void>

    // GET
    @GET("/data")
    fun fetchData(@Header("Authorization") token: String): Call<UserResponse>

    @GET("/get_auth_drivers")
    fun getAuthDrivers(@Header("Authorization") token: String): Call<List<DriverVecLocModel>>

    @GET("/get_available_forestry_drivers")
    fun getAvailableForestryDrivers(): Call<List<DriverVecLocModel>>

    @GET("/get_available_rural_drivers")
    fun getAvailableRuralDrivers(): Call<List<DriverVecLocModel>>

    @GET("/get_forestry_petition")
    fun getForestryPetition(@Header("Authorization") token: String): Call<Int>

    @GET("/get_incoming_passengers")
    fun getIncomingPassengers(@Header("Authorization") token: String): Call<Int>

    @GET("/get_is_queued/{userId}")
    fun getisQueued(@Path("userId") userId: Int): Call<Boolean>

    @GET("/get_locations")
    fun getLocations(): Call<List<LocationModel>>

    @GET("/get_pending_drivers")
    fun getPendingDrivers(@Header("Authorization") token: String): Call<List<DriverVecLocModel>>

    @GET("/get_petition")
    fun getPetition(@Header("Authorization") token: String): Call<Int>

    @GET("/get_rural_petition")
    fun getRuralPetition(@Header("Authorization") token: String): Call<Int>

    @GET("is_authorized/{userId}")
    fun isAuthorized(@Path("userId") userId: Int): Call<Boolean>

    // POST
    @POST("/add_location")
    fun addLocation(@Header("Authorization") token: String, @Body newLocation: LocationModel): Call<Void>

    @POST("/add_queued_user/{vehicleId}/{userId}")
    fun addQueuedUser(@Path("vehicleId") vehicleId: Int, @Path("userId") userId: Int): Call<Void>

    @POST("/google-sign-in-endpoint")
    fun signInWithGoogle(@Body request: LoginResponse): Call<String>

    @POST("/google-sign-up-endpoint")
    fun signUpWithGoogle(@Body request: LoginResponse): Call<String>

    @POST("/register_driver")
    fun registerDriver(@Body request: DriverSignUpRequest): Call<EditUserResponse>

    @POST("/register_user")
    fun registerUser(@Body newUser: UserModel): Call<EditUserResponse>

    @POST("/login")
    fun login(@Body credentials: Credentials): Call<LoginResponse>

    @POST("/signin")
    fun signIn(@Body newUser: UserModel): Call<UserResponse>

    // PUT
    @PUT("/change_is_queued/{userId}")
    fun changeisQueued(@Path("userId") userId: Int): Call<Void>

    @PUT("/edit_user/{userId}")
    fun editUser(@Path("userId") userId: Int, @Body user: EditUserModel): Call<EditUserResponse>

    @PUT("/ready_driver")
    fun readyDriver(@Header("Authorization") token: String): Call<Void>

    @PUT("/remove_queued_user/{vehicleId}/{userId}")
    fun removeQueuedUser(@Path("vehicleId") vehicleId: Int, @Path("userId") userId: Int): Call<Void>

    @PUT("/take_passengers")
    fun takePassengers(@Header("Authorization") token: String): Call<Void>

    @PUT("/update_authorized/{userId}")
    fun updateAuthorizedStatus(@Path("userId") userId: Int): Call<Void>

    @PUT("/add_forestry_petition")
    fun addForestryPetition(@Header("Authorization") token: String): Call<Void>

    @PUT("/add_rural_petition")
    fun addRuralPetition(@Header("Authorization") token: String): Call<Void>
}
