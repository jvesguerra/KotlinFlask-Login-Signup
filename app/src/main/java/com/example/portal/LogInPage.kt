package com.example.portal

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.Credentials
import com.example.portal.models.LoginResponse
import com.example.portal.models.UserModel
import com.example.portal.models.UserResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await


class LogInPage : Fragment() {
    private val RC_SIGN_IN = 123 // Replace with any unique request code
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)
    private var yourWebClientId: String = "562377295927-26r2kaucq403vbpo01pd5bjq9volo46n.apps.googleusercontent.com"

    // Login and Sign up variables
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button

//    private lateinit var signInButtonGoogle: Button
//    private lateinit var signUpButtonGoogle: Button


    private lateinit var view: View
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)


        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.login_page, container, false)

        emailEditText = view.findViewById(R.id.editTextEmail)
        passwordEditText = view.findViewById(R.id.editTextPassword)
        signInButton = view.findViewById(R.id.buttonSignIn)
        signUpButton = view.findViewById(R.id.buttonSignUp)
        //signInButtonGoogle = view.findViewById(R.id.signInButtonGoogle)
        //signUpButtonGoogle = view.findViewById(R.id.signUpButtonGoogle)

        // REMOVE
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        //Toast.makeText(requireContext(), "IsLoggedIn: $isLoggedIn", Toast.LENGTH_SHORT).show()

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            signIn(email, password)
        }

        signUpButton.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.signup)
        }

//        signInButtonGoogle.setOnClickListener {
//            // Start Google Sign-In process for sign-up
//            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your web client ID
//                .requestEmail()
//                .build()
//
//            val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
//            val signInIntent = googleSignInClient.signInIntent
//            startActivityForResult(signInIntent, RC_SIGN_IN)
//
//        }
//
//        signUpButtonGoogle.setOnClickListener {
//            // Start Google Sign-In process for sign-up
//            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your web client ID
//                .requestEmail()
//                .build()
//
//            val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
//            val signInIntent = googleSignInClient.signInIntent
//            startActivityForResult(signInIntent, RC_SIGN_IN)
//
//        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                sendIdTokenToBackend(idToken, "signUp")
            } else {
                Log.e("LogInPage", "Google ID token is null")
            }

        } catch (e: ApiException) {
            Log.e("LogInPage", "signInResult:failed code=${e.statusCode}")
        }
    }

    private fun sendIdTokenToBackend(idToken: String, action: String) {
        val retrofitService = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)

        when (action) {
            "signIn" -> {
                val call = retrofitService.signInWithGoogle(LoginResponse(idToken))
                call.enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        // Handle successful sign-in response from the backend
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        // Handle failure to send ID token to the backend for sign-in
                    }
                })
            }
            "signUp" -> {
                val call = retrofitService.signUpWithGoogle(LoginResponse(idToken))
                call.enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        // Handle successful sign-in response from the backend
                        Navigation.findNavController(requireView()).navigate(R.id.userHome)
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        // Handle failure to send ID token to the backend for sign-in
                    }
                })
            }
            else -> {
                // Invalid action
            }
        }
    }

    private fun saveLoginSession(userType: Int,userId: Int, firstName: String, lastName: String) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putInt("userType", userType)
        editor.putInt("userId", userId)
        editor.putString("firstName", firstName)
        editor.putString("lastName", lastName)
        editor.apply()
    }

    private fun signIn(email: String, password: String) {
        val call = retrofitService.login(Credentials(email, password))
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val accessToken = loginResponse?.accessToken
                    val editor = sharedPreferences.edit()
                    editor?.putString("accessToken", accessToken)
                    editor?.apply()
                    Log.d("LogInPage", "AccessToken: $accessToken")
                    fetchUserData(accessToken)
                } else {
                    showToast("Failed to sign in: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showToast("Failed to sign in: ${t.message}")
            }
        })
    }

    private fun fetchUserData(accessToken: String?) {
        if (accessToken != null) {
            val dataCall = retrofitService.fetchData("Bearer $accessToken")
            dataCall.enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        userResponse?.let { handleSignInSuccess(it) }
                    } else {
                        // Failed to fetch user data
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    // Error handling
                }
            })
        }
    }


    private fun handleSignInSuccess(userResponse: UserResponse) {
        val user = userResponse.user
        user?.let {
            val userType = it.userType
            val userId = 0
            val firstName = it.firstName
            val lastName = it.lastName
            saveLoginSession(userType, userId, firstName,lastName)

            when (userType) {
                0 -> Navigation.findNavController(requireView()).navigate(R.id.home)
                1 -> Navigation.findNavController(requireView()).navigate(R.id.userHome)
                2 -> {
                    var isAuthorized = false
                    retrofitService.isAuthorized(userId).enqueue(object : Callback<Boolean> {
                        override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                            if (response.isSuccessful && response.body() != null) {
                                isAuthorized = response.body()!!
                                if (isAuthorized) {
                                    Navigation.findNavController(requireView()).navigate(R.id.toDriverHome)
                                } else {
                                    Navigation.findNavController(requireView()).navigate(R.id.unauthorized)
                                }
                            } else {
                                // Handle error case
                            }
                        }

                        override fun onFailure(call: Call<Boolean>, t: Throwable) {
                            // Handle failure case
                        }
                    })
                }
            }
        } ?: showToast("Failed to retrieve user details")
    }

    public fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

private fun <T> Call<T>.enqueue(callback: Callback<LoginResponse>) {

}

fun Call<Void>.enqueueVoid(callback: () -> Unit) {
    this.enqueue(object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if (response.isSuccessful) {
                callback.invoke()


            } else {
                println("Error: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            println("Network request failed: ${t.message}")
        }
    })
}

fun <T> Call<T>.enqueue(callback: (Response<T>) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            callback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            Log.e("Network request failed", t.message ?: "Unknown error")
        }
    })
}
