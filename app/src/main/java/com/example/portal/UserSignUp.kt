package com.example.portal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.Navigation

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UserSignUp : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var view: View
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)

    private lateinit var emailEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var contactNumberEditText: EditText
    private lateinit var passwordEditText: EditText

    private lateinit var plateNumberEditText: EditText
    private lateinit var routeEditText: EditText

    private lateinit var buttonSignUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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
        )
        retrofitService.register(newUser).enqueueVoid {
            Navigation.findNavController(view).navigate(R.id.toUserHome1)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UserSignUp().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}