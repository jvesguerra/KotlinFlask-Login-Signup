package com.example.portal.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.portal.R
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.DriverSignUpRequest
import com.example.portal.models.UserModel
import com.example.portal.models.VehicleModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DriverSignUp : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var view: View
    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var contactNumberEditText: EditText
    private lateinit var plateNumberEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var buttonSignUp: Button

    private lateinit var routeEditText: Spinner
    var selectedItem: String = ""

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
        view = inflater.inflate(R.layout.driver_signup, container, false)

        firstNameEditText = view.findViewById(R.id.editTextFirstName)
        lastNameEditText = view.findViewById(R.id.editTextLastName)
        emailEditText = view.findViewById(R.id.editTextEmail)
        contactNumberEditText = view.findViewById(R.id.editTextContactNumber)
        passwordEditText = view.findViewById(R.id.editTextPassword)

        plateNumberEditText = view.findViewById(R.id.editTextPlateNumber)
        buttonSignUp = view.findViewById(R.id.buttonSignUp)

        routeEditText = view.findViewById(R.id.editTextRoute)
        // Create an ArrayAdapter using a string array and a default spinner layout
        val routeAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.route_options,
            R.layout.custom_spinner_item
        )
        //  Specify the layout to use when the list of choices appears
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        routeEditText.adapter = routeAdapter

        routeEditText.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedItem = parent.getItemAtPosition(position).toString()
                // Use selectedItem as needed
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing here
            }
        }

        buttonSignUp.setOnClickListener {
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val contactNumber = contactNumberEditText.text.toString()
            val password = passwordEditText.text.toString()

            val route = selectedItem
            val plateNumber = plateNumberEditText.text.toString()

            signUp(email, firstName, lastName, contactNumber, password, route, plateNumber)
        }

        return view
    }


    private fun signUp(email: String, firstName: String, lastName: String, contactNumber: String, password: String, route: String, plateNumber: String) {
            val newUser = UserModel(
                userId = 0,
                firstName = firstName,
                lastName = lastName,
                email = email,
                contactNumber = contactNumber,
                password = password,
                userType = 2,
                rating = 0,
                isActive = true,
                authorized = false,
                isQueued = false,
                isPetitioned = 0
            )

            val newVehicle = VehicleModel(
                vehicleId = 0,
                userId = 0,
                plateNumber = plateNumber,
                route = route,
                isAvailable = false,
                hasDeparted = false,
                isFull = false,
                queuedUsers = mutableListOf()
            )

            val request = DriverSignUpRequest(newUser, newVehicle)

            retrofitService.registerDriver(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        println("User and Vehicle registered successfully")
                        Navigation.findNavController(view).navigate(R.id.toUnauthorized)
                    } else {
                        println("Failed to register user and vehicle")
                        // Handle error
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    println("Failed to register user and vehicle: ${t.message}")
                    // Handle failure
                }
            })
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DriverSignUp().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
