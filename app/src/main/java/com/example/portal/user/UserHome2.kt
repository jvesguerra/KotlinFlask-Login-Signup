package com.example.portal.user

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.Adapter
import com.example.portal.R
import com.example.portal.api.OnDeleteUserListener
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.SessionManager
import com.example.portal.api.UserServe
import com.example.portal.functions.UserDeletion
import com.example.portal.models.DriverVehicleModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserHome2 : Fragment(), OnDeleteUserListener {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Adapter

    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_home2, container, false)
        val route = arguments?.getString("route")

        sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val logoutButton: Button = view.findViewById(R.id.btnLogout)

        logoutButton.setOnClickListener {
            signOut()
            Navigation.findNavController(view).navigate(R.id.logout)
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = Adapter(this@UserHome2,requireContext(),  Adapter.ContextType.USER_HOME2,mutableListOf())
        recyclerView.adapter = adapter

        val call: Call<List<DriverVehicleModel>> = if (route == "Forestry"){
            retrofitService.getAvailableForestryDrivers()
        }else{
            retrofitService.getAvailableRuralDrivers()
        }

        call.enqueue(object : Callback<List<DriverVehicleModel>> {
            override fun onResponse(call: Call<List<DriverVehicleModel>>, response: Response<List<DriverVehicleModel>>) {
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    adapter.updateData(items) // Update the adapter's data
                }
            }

            override fun onFailure(call: Call<List<DriverVehicleModel>>, t: Throwable) {
                Log.e("UserHome2", "Error fetching data", t)
            }
        })

        return view
    }
    private fun signOut() {
        SessionManager.signOut(requireContext())
    }

    override fun onDeleteUser(userId: Int, position: Int) {
        val userDeletion = UserDeletion(requireContext(), adapter)
        userDeletion.deleteUser(retrofitService, userId, position)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UserHome2().apply {

            }
    }
}