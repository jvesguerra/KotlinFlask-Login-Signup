package com.example.portal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.Navigation

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [user_signup.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserSignUp : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var view: View
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance().create(UserServe::class.java)

    private lateinit var fullNameEditText: EditText
    private lateinit var contactNumberEditText: EditText
    private lateinit var routeEditText: EditText
    private lateinit var plateNumberEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
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
    ): View? {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.user_signup, container, false)

        fullNameEditText = view.findViewById(R.id.editTextFullName)
        contactNumberEditText = view.findViewById(R.id.editTextContactNumber)
        emailEditText = view.findViewById(R.id.editTextEmail)
        passwordEditText = view.findViewById(R.id.editTextPassword)
        buttonSignUp = view.findViewById(R.id.buttonSignUp)

        buttonSignUp.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val contactNumber = contactNumberEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // You can use the above values as needed, for example, pass them to a signup function
            signUp(email, fullName, password,)

        }

        return view
    }

    private fun signUp(email: String, fullname: String, password: String) {
        // TODO: Implement sign-up logic
        val newUser = UserModel(
            userId = 0,
            fullname = fullname,
            email = email,
            password = password,
            userType = 1,
            locationId = 0
        )
        retrofitService.register(newUser).enqueueVoid {
            println("Sign Up")
            Navigation.findNavController(view).navigate(R.id.toUserHome1)
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment user_signup.
         */
        // TODO: Rename and change types and number of parameters
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