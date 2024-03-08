package com.example.portal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
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
import android.widget.EditText
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.portal.GoogleSignInRequest
import com.example.portal.UserModel

class BlankFragment : Fragment() {
    private val RC_SIGN_IN = 123 // Replace with any unique request code
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)
    private var yourWebClientId: String = "562377295927-0rk3t4op4e7kb0mufqif63o0s7gob42i.apps.googleusercontent.com"

    // Login and Sign up variables
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button

    private lateinit var view: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_blank, container, false)

        emailEditText = view.findViewById(R.id.editTextEmail)
        passwordEditText = view.findViewById(R.id.editTextPassword)
        signInButton = view.findViewById(R.id.buttonSignIn)
        signUpButton = view.findViewById(R.id.buttonSignUp)

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

        return view
    }

    // The rest of your methods remain the same...
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
            Navigation.findNavController(view).navigate(R.id.action_mainActivity_to_yourFragment)

        }
    }

    private fun signUp(email: String, password: String) {
        // TODO: Implement sign-up logic
        val newUser = UserModel(
            userId = 55,
            fullname = "Shula Carvajal",
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
