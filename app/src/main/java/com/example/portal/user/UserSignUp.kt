package com.example.portal.user

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
import com.example.portal.R
import com.example.portal.models.UserModel
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.enqueue
import com.example.portal.enqueueVoid
import com.example.portal.models.EditUserResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserSignUp : Fragment() {
    private lateinit var view: View
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    private lateinit var emailEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var contactNumberEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var buttonSignUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.user_signup, container, false)

        emailEditText = view.findViewById(R.id.editTextEmail)
        firstNameEditText = view.findViewById(R.id.editTextFirstName)
        lastNameEditText = view.findViewById(R.id.editTextLastName)
        contactNumberEditText = view.findViewById(R.id.editTextContactNumber)
        passwordEditText = view.findViewById(R.id.editTextPassword)

        buttonSignUp = view.findViewById(R.id.buttonSignUp)

        buttonSignUp.setOnClickListener {
            val email = emailEditText.text.toString()
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val contactNumber = contactNumberEditText.text.toString()
            val password = passwordEditText.text.toString()

            signUp(email, firstName, lastName, contactNumber, password)

        }

        return view
    }

    private fun signUp(email: String, firstName: String, lastName: String, contactNumber: String, password: String) {
        val newUser = UserModel(
            userId = 0,
            firstName = firstName,
            lastName = lastName,
            email = email,
            contactNumber = contactNumber,
            password = password,
            userType = 1,
            rating = 0,
            isActive = true,
            authorized = false,
            isQueued = false,
            isPetitioned = 0
        )

        val call = retrofitService.registerUser(newUser)
        call.enqueue(object : Callback<EditUserResponse> {
            override fun onResponse(call: Call<EditUserResponse>, response: Response<EditUserResponse>) {
                if (response.isSuccessful) {
                    Log.e("Success", "User successfully registered")
                    val message = "User successfully registered"
                    Navigation.findNavController(view).navigate(R.id.toUserHome1)
                    showToast(message)
                } else {
                    val errorJson = response.errorBody()?.string()
                        ?.let { JSONObject(it) }
                    val errorMessage = errorJson?.getString("message")
                    // Display error message
                    Log.e("Error", "Response error: $errorMessage")
                    if (errorMessage != null) {
                        showToast(errorMessage)
                    }
                }
            }

            override fun onFailure(call: Call<EditUserResponse>, t: Throwable) {
                try {
                    // Handle network error
                    Log.e("Network Error", "Error: ${t.message}", t)
                } catch (e: Exception) {
                    // Handle any other unexpected exceptions
                    Log.e("Error", "Unexpected error: ${e.message}", e)
                }
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}