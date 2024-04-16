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
import com.example.portal.models.UserModel
import com.example.portal.models.UserResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
    private var yourWebClientId: String = "562377295927-0rk3t4op4e7kb0mufqif63o0s7gob42i.apps.googleusercontent.com"

    // Login and Sign up variables
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button

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

        // REMOVE
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        Toast.makeText(requireContext(), "IsLoggedIn: $isLoggedIn", Toast.LENGTH_SHORT).show()

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            signIn(email, password)
        }

        signUpButton.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.signup)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun saveLoginSession(userType: Int,userId: Int) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putInt("userType", userType) // Save user type in SharedPreferences
        editor.putInt("userId", userId) // Save user type in SharedPreferences
        editor.apply()
    }

    private fun signIn(email: String, password: String) {
        val newUser = UserModel(
            userId = 0,
            firstName = "admin",
            lastName = "admin",
            email = email,
            contactNumber = "",
            password = password,
            rating = 0,
            userType = 0,
            isActive = true,
            authorized = false,
        )

        val call = retrofitService.signIn(newUser)
        call.enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    userResponse?.let { handleSignInSuccess(it) }
                } else {
                    showToast("Failed to sign in: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                showToast("Failed to sign in: ${t.message}")
            }
        })
    }

    private fun handleSignInSuccess(userResponse: UserResponse) {
        val user = userResponse.user
        user?.let {
            val userType = it.userType
            val userId = it.userId
            saveLoginSession(userType,userId)
            val checkId = "USER ID: $userId"
            showToast(checkId)

            val stringNumber: String = userType.toString()

            if (userType == 0) {
                Navigation.findNavController(view).navigate(R.id.home)
            // user homepage
            }else if(userType == 1){
                Navigation.findNavController(view).navigate(R.id.userHome)
            // driver homepage
            }else if(userType == 2){
                Navigation.findNavController(view).navigate(R.id.toDriverHome)
            }

        } ?: showToast("Failed to retrieve user details")
    }
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                // Now, you have the Google ID token; proceed to send it to the backend
                sendIdTokenToBackend(idToken)
            } else {
                Log.e("MainActivity", "Google ID token is null")
            }

        } catch (e: ApiException) {
            // Handle sign-in failure
            Log.e("MainActivity", "signInResult:failed code=${e.statusCode}")
        }
    }

    private fun sendIdTokenToBackend(idToken: String) {
        // Use Retrofit to send the Google ID token to your backend
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = retrofitService.signInWithGoogle(GoogleSignInRequest(idToken)).await()

                // Handle the response from the Flask backend, e.g., update UI
                Log.d("MainActivity", "Response from server: $response")

            } catch (e: Exception) {
                // Handle network issues or other failures
                Log.e("MainActivity", "Error during Google Sign-In: $e")
            }
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
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
