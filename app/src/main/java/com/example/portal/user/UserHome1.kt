package com.example.portal.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation
import com.example.portal.R

class UserHome1 : Fragment() {
    private lateinit var view: View

    private lateinit var btnForestry: Button
    private lateinit var btnRural: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.user_home1, container, false)

        btnForestry = view.findViewById(R.id.btnForestry)
        btnRural = view.findViewById(R.id.btnRural)
        val bundle = Bundle()

        btnForestry.setOnClickListener {
            bundle.putString("route", "Forestry") // Add "forestry" to the bundle
            Navigation.findNavController(view).navigate(R.id.toAvailableVehicles, bundle)
        }

        btnRural.setOnClickListener {
            bundle.putString("route", "Rural")
            Navigation.findNavController(view).navigate(R.id.toAvailableVehicles, bundle)
        }

        return view
    }
}