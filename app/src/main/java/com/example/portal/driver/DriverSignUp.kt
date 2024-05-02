package com.example.portal.driver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.portal.R
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.models.DriverSignUpRequest
import com.example.portal.models.EditUserResponse
import com.example.portal.models.UserModel
import com.example.portal.models.VehicleModel
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DriverSignUp : Fragment() {
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

    private lateinit var emailErrorText: TextView
    private lateinit var firstNameErrorText: TextView
    private lateinit var lastNameErrorText: TextView
    private lateinit var contactNumberErrorText: TextView
    private lateinit var passwordErrorText: TextView
    private lateinit var plateNumberErrorText: TextView

    private lateinit var routeEditText: Spinner
    var selectedItem: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // ERROR MESSAGES
        emailErrorText = view.findViewById(R.id.emailError)
        firstNameErrorText = view.findViewById(R.id.firstNameError)
        lastNameErrorText = view.findViewById(R.id.lastNameError)
        contactNumberErrorText = view.findViewById(R.id.contactNumberError)
        passwordErrorText = view.findViewById(R.id.passwordError)
        plateNumberErrorText = view.findViewById(R.id.plateNumberError)

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
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
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

            // Check for missing data first
            val missingFields = mutableListOf<String>()
            if (firstName.isEmpty()) {
                missingFields.add("First Name")
            }
            if (lastName.isEmpty()) {
                missingFields.add("Last Name")
            }
            if (email.isEmpty()) {
                missingFields.add("Email")
            }
            if (contactNumber.isEmpty()) {
                missingFields.add("Contact Number")
            }
            if (password.isEmpty()) {
                missingFields.add("Password")
            }
            if (route.isEmpty()) {
                missingFields.add("Route") // Assuming route is required
            }
            if (plateNumber.isEmpty()) {
                missingFields.add("Plate Number") // Assuming plate number is required
            }

            if (missingFields.isNotEmpty()) {
                val errorMessage =
                    "Please fill in the following fields: ${missingFields.joinToString(", ")}"
                showToast(errorMessage)
            } else {
                if (containsDigits(firstName)) {
                    showToast("Invalid First Name format")
                    firstNameErrorText.text = "Invalid First name format"
                } else {
                    if (containsDigits(lastName)) {
                        showToast("Invalid Last Name format")
                        lastNameErrorText.text = "Invalid Last name format"
                    } else {
                        if (isValidContactNumber(contactNumber)) {
                            if (isValidPassword(password)) {
                                if (isValidPlateNumber(plateNumber)) {
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                                            .matches()
                                    ) {
                                        showToast("Invalid email format")
                                        emailErrorText.text = "Invalid email format"
                                    } else {
                                        signUp(
                                            email,
                                            firstName,
                                            lastName,
                                            contactNumber,
                                            password,
                                            route,
                                            plateNumber
                                        )
                                    }
                                } else {
                                    showToast("Invalid plate number format")
                                    plateNumberErrorText.text = "Invalid plate number format"
                                }
                            } else {
                                showToast("Invalid password format")
                                passwordErrorText.text = "Invalid password format"
                            }
                        } else {
                            showToast("Invalid contact number format")
                            contactNumberErrorText.text = "Invalid contact number format"
                        }
                    }
                }
            }


        }

        return view
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidContactNumber(contactNumber: String): Boolean {
        val pattern = Pattern.compile("^(09|\\+639|\\+63 9)[0-9]{2}-?[0-9]{3}-?[0-9]{4}\$")
        val matcher = pattern.matcher(contactNumber)
        return matcher.matches()
    }

    private fun isValidPlateNumber(plateNumber: String): Boolean {
        val pattern = Pattern.compile("^[a-zA-Z0-9]{3}\\s\\d{3,4}\$")
        val matcher = pattern.matcher(plateNumber)
        return matcher.matches()
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*-])[A-Za-z\\d!@#$%^&*-]+$")
        val matcher = pattern.matcher(password)
        return matcher.matches()
    }

    private fun containsDigits(name: String): Boolean {
        val regex = Regex("\\d")  // Raw string for digit pattern
        return regex.containsMatchIn(name)
    }

    private fun signUp(
        email: String,
        firstName: String,
        lastName: String,
        contactNumber: String,
        password: String,
        route: String,
        plateNumber: String
    ) {
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

        val call = retrofitService.registerDriver(request)
        call.enqueue(object : Callback<EditUserResponse> {
            override fun onResponse(
                call: Call<EditUserResponse>,
                response: Response<EditUserResponse>
            ) {
                if (response.isSuccessful) {
                    Log.e("Success", "Driver successfully registered")
                    val message = "Driver successfully registered"
                    showToast(message)
                    Navigation.findNavController(view).navigate(R.id.toUnauthorized)
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
}
