package com.example.portal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SignUpPage : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

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
        val view = inflater.inflate(R.layout.signup_page, container, false)
        val btnDriver: Button = view.findViewById(R.id.toDriverSignUp)
        val btnUser: Button = view.findViewById(R.id.toUserSignUp)

        btnDriver.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toDriverSignUp)
        }
        btnUser.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.toUserSignUp)
        }
        return view
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SignUpPage().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}