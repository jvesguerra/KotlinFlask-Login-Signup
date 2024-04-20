package com.example.portal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.portal.api.OnDeleteUserListener
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.api.OnQueueUserListener
import com.example.portal.models.DriverVecLocModel
import retrofit2.Response
import retrofit2.Call
import retrofit2.Callback

class Adapter(
    private val onDeleteUserListener: OnDeleteUserListener?,
    private val onQueueUserListener: OnQueueUserListener?,
    private val context: Context,
    private val contextType: ContextType,
    private var items: MutableList<DriverVecLocModel>
) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
        .create(UserServe::class.java)
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("userId", 0)

    enum class ContextType {
        ADMIN_HOME, PENDING_LISTS, USER_HOME2
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(item.userId, position)
        }

        holder.authorizeButton.setOnClickListener {
            showAuthorizeConfirmationDialog(item.userId)
        }

        holder.showDriverDetails.setOnClickListener {
            showDriverDetailsDialog(item)
        }

//        holder.queueButton.setOnClickListener {
//            val route = item.route
//            val vehicleId = item.longitude
//            val contactNumber = item.contactNumber
//            Log.d("VEHICLE", "Route: $route")
//            Log.d("VEHICLE", "Contact Number: $contactNumber") // Omitting vehicle ID for privacy
//            Log.d("VEHICLE", "Vehicle ID: $vehicleId") // Omitting vehicle ID for privacy
//            showQueueConfirmationDialog(userId, position, item.vehicleId)
//        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun removeItemAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateData(newItems: List<DriverVecLocModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun deleteUser(userId: Int, position: Int) {
        onDeleteUserListener?.onDeleteUser(userId, position)
    }

    private fun queueUser(userId: Int, position: Int, vehicleId: Int) {
        onQueueUserListener?.onQueueUser(userId, position, vehicleId)
    }

    private fun removeUserQueue(userId: Int, position: Int, vehicleId: Int) {
        onQueueUserListener?.onRemoveUserQueue(userId, position, vehicleId)
    }

    private fun showQueueConfirmationDialog(userId: Int, position: Int, vehicleId: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Queue Driver")
        builder.setMessage("Are you sure you want to queue this driver?")
        builder.setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
            queueUser(userId, position, vehicleId)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    private fun showCancelQueueConfirmationDialog(userId: Int, position: Int, vehicleId: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Cancel Queue")
        builder.setMessage("Are you sure you want to cancel your queue?")
        builder.setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
            removeUserQueue(userId, position, vehicleId)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    private fun showDeleteConfirmationDialog(userId: Int, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirm Delete")
        builder.setMessage("Are you sure you want to delete this user?")
        builder.setPositiveButton("Delete") { dialogInterface: DialogInterface, i: Int ->
            deleteUser(userId, position)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    private fun showAuthorizeConfirmationDialog(userId: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirm Authorization")
        builder.setMessage("Are you sure you want to authorize this user?")
        builder.setPositiveButton("Authorize") { dialogInterface: DialogInterface, i: Int ->
            updateAuthorizedStatus(userId)
            dialogInterface.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    private fun showDriverDetailsDialog(user: DriverVecLocModel) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_driver_detail, null)

        dialogView.findViewById<TextView>(R.id.fullNameTextView).text = "${user.firstName} ${user.lastName}"
        dialogView.findViewById<TextView>(R.id.userIdTextView).text = "Contact Number: ${user.vehicleId}"
        dialogView.findViewById<TextView>(R.id.plateNumberTextView).text = "Plate Number: ${user.plateNumber}"
        dialogView.findViewById<TextView>(R.id.routeTextView).text = "Route: ${user.route}"

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        builder.setTitle("Driver Details")

        if (contextType == ContextType.PENDING_LISTS) {
            builder.setTitle("Confirm Authorization")
            builder.setPositiveButton("Authorize") { dialogInterface: DialogInterface, i: Int ->
                updateAuthorizedStatus(user.userId)
                dialogInterface.dismiss()
            }
        }
        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    private fun updateAuthorizedStatus(userId: Int) {
        retrofitService.updateAuthorizedStatus(userId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Update local data if needed
                    // For example, find the corresponding DriverVehicle object in items list and update its authorized value
                } else {
                    // Handle API error
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle network error
            }
        })
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val authorizeButton: Button = itemView.findViewById(R.id.authorizeButton)
        val showDriverDetails: Button = itemView.findViewById(R.id.showDriverDetails)
        val queueButton: Button = itemView.findViewById(R.id.queueButton)
        private var isUserQueued: Boolean = false

        private val isFullTextView: TextView = itemView.findViewById(R.id.IsFullTextView)
        private val hasDepartedTextView: TextView = itemView.findViewById(R.id.HasDepartedTextView)

        // determine if vehicle is in terminal
        private fun isPointInPolygon(point: Pair<Double, Double>, vertices: List<Pair<Double, Double>>): Boolean {
            val (px, py) = point
            var isInside = false

            var i = 0
            var j = vertices.size - 1

            while (i < vertices.size) {
                val (vertexI_x, vertexI_y) = vertices[i]
                val (vertexJ_x, vertexJ_y) = vertices[j]

                if ((vertexI_y > py) != (vertexJ_y > py) &&
                    px < (vertexJ_x - vertexI_x) * (py - vertexI_y) / (vertexJ_y - vertexI_y) + vertexI_x
                ) {
                    isInside = !isInside
                }

                j = i++
            }

            return isInside
        }

        fun isWithinSquare(lat: Double, long: Double, topLeftLat: Double, topLeftLong: Double, bottomRightLat: Double, bottomRightLong: Double): Boolean {
            return (topLeftLat >= lat && lat >= bottomRightLat) && (topLeftLong <= long && long <= bottomRightLong)
        }




        fun bind(user: DriverVecLocModel) {
            val fullname = "${user.firstName} ${user.lastName}"
            itemView.findViewById<TextView>(R.id.itemNameTextView).text = fullname
            itemView.findViewById<TextView>(R.id.RouteTextView).text = user.plateNumber
            itemView.findViewById<TextView>(R.id.PlateNumberTextView).text = user.route


            // Chooses which buttons to Show
            when (contextType) {
                ContextType.ADMIN_HOME -> {
                    deleteButton.visibility = View.VISIBLE
                    showDriverDetails.visibility = View.VISIBLE

                    authorizeButton.visibility = View.GONE
                    queueButton.visibility = View.GONE

                    isFullTextView.visibility = View.GONE
                    hasDepartedTextView.visibility = View.GONE
                }
                ContextType.USER_HOME2 -> {
                    val isFull = if(user.isFull) "Full" else "Not Full"

                    var vertices = listOf<Pair<Double, Double>,>()
                    if (user.route == "Forestry") {
                        vertices = listOf(
                            Pair(14.168132, 121.242189), // Top Left
                            Pair(14.167896, 121.242343), // Bottom Left
                            Pair(14.168105, 121.243113), // Bottom Right
                            Pair(14.168517, 121.243002)  // Top Right
                        )
                        Log.d("VERTICES", "Forestry")
                        val checkInside = isWithinSquare(user.latitude,
                            user.longitude,14.168132,121.242189,
                            14.168105,121.243113)
                        Log.d("isInside", "LAT: ${user.latitude} LONG: ${user.longitude} = $checkInside")
                    } else {
                        vertices = listOf(
                            Pair(14.166140, 121.243435), // Top Left
                            Pair(14.165353, 121.244118), // Bottom Left
                            Pair(14.166247, 121.244094), // Bottom Right
                            Pair(14.165501, 121.244475)  // Top Right
                        )
                        Log.d("VERTICES", "Rural")
                    }
                    // check gps coordinates here
                    val deviceLocation = Pair(user.latitude, user.longitude)
                    val isInside = isPointInPolygon(deviceLocation, vertices)
                    Log.d("LocationCheck", "isInside Terminal: $isInside")
                    val hasDeparted = if(isInside) "Not Departed" else "Departed"
                    isFullTextView.text = isFull
                    hasDepartedTextView.text = hasDeparted

                    queueButton.visibility = View.VISIBLE
                    isFullTextView.visibility = View.VISIBLE
                    hasDepartedTextView.visibility = View.VISIBLE

                    authorizeButton.visibility = View.GONE
                    deleteButton.visibility = View.GONE
                    showDriverDetails.visibility = View.VISIBLE

                    retrofitService.getisQueued(userId).enqueue(object : Callback<Boolean> {
                        override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                            if (response.isSuccessful && response.body() != null) {
                                isUserQueued = response.body()!!
                                if (isUserQueued) {
                                    queueButton.text = "Cancel Queue"
                                } else {
                                    queueButton.text = "Queue"
                                }
                            } else {
                                // Handle error case
                                // For example, if there's a network issue or server error
                            }
                        }

                        override fun onFailure(call: Call<Boolean>, t: Throwable) {
                            // Handle failure case
                            // For example, if the request failed due to a network issue
                        }
                    })

                    queueButton.setOnClickListener {
                        if (isUserQueued) {
                            // If the user is already queued, show confirmation dialog to cancel the queue
                            showCancelQueueConfirmationDialog(userId, position, user.vehicleId)
                        } else {
                            // If the user is not queued, show confirmation dialog to queue the user
                            showQueueConfirmationDialog(userId, position, user.vehicleId)
                        }
                    }
                }
                ContextType.PENDING_LISTS -> {
                    authorizeButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.VISIBLE
                    showDriverDetails.visibility = View.VISIBLE

                    queueButton.visibility = View.GONE

                    isFullTextView.visibility = View.GONE
                    hasDepartedTextView.visibility = View.GONE
                }
            }
        }
    }
}