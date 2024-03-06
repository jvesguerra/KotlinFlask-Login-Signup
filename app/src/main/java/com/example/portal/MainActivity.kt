package com.example.portal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
//import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await
import android.widget.EditText

class MainActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123 // Replace with any unique request code
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)
    private var yourWebClientId: String = "562377295927-0rk3t4op4e7kb0mufqif63o0s7gob42i.apps.googleusercontent.com"

    // Login and Sign up variables
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        signInButton = findViewById(R.id.buttonSignIn)
        signUpButton = findViewById(R.id.buttonSignUp)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(yourWebClientId) // Replace with your web client ID from the Google Cloud Console
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

//        val signInButton = findViewById<Button>(R.id.signInButton)
//        // Set a click listener for the sign-in button
//        signInButton.setOnClickListener {
//            val signInIntent = googleSignInClient.signInIntent
//            startActivityForResult(signInIntent, RC_SIGN_IN)
//        }

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            signIn(email, password)
        }

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            signUp(email, password)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun signIn(email: String, password: String) {
        // TODO: Implement sign-in logic
        val newUser = UserModel(
            userId = 0,
            fullname = "admin",
            email = email,
            password = password,
            type = 0,
            locationId = 0
        )
        retrofitService.signIn(newUser).enqueueVoid {
                println("Sign in")
            }
    }

    private fun signUp(email: String, password: String) {
        // TODO: Implement sign-up logic
        val newUser = UserModel(
            userId = 55,
            fullname = "Joshua Esguerra",
            email = email,
            password = password,
            type = 0,
            locationId = 0
        )
        retrofitService.register(newUser).enqueueVoid {
            println("Sign Up")
        }
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

