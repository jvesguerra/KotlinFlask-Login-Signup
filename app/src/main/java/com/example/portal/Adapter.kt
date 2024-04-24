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
import android.widget.EditText
import com.example.portal.api.OnDeleteUserListener
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import com.example.portal.api.OnQueueUserListener
import com.example.portal.models.DriverVecLocModel
import com.example.portal.models.EditUserModel
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

        holder.editButton.setOnClickListener {
            showEditDialog(context, item.userId, position, item.vehicleId)
        }


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

    private fun editUser(userId: Int, position: Int, userModel: EditUserModel) {
        onQueueUserListener?.editUser(userId, position, userModel)
    }

    private fun showEditDialog(context: Context,userId: Int, position: Int, vehicleId: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edit Driver Details")

        val inflater = LayoutInflater.from(context)
        val dialogLayout = inflater.inflate(R.layout.edit_user_dialog_layout, null)

        val emailEditText = dialogLayout.findViewById<EditText>(R.id.editTextEmail)
        val firstNameEditText = dialogLayout.findViewById<EditText>(R.id.editTextFirstName)
        val lastNameEditText = dialogLayout.findViewById<EditText>(R.id.editTextLastName)

        builder.setView(dialogLayout)

        builder.setPositiveButton("Save") { dialogInterface: DialogInterface, i: Int ->
            val editedEmail = emailEditText.text.toString()
            val editedFirstName = firstNameEditText.text.toString()
            val editedLastName = lastNameEditText.text.toString()

            val editedUser = EditUserModel(
                email = editedEmail,
                firstName = editedFirstName,
                lastName = editedLastName
            )

            Log.d("EditUser", "Email: $editedEmail, First Name: $editedFirstName, Last Name: $editedLastName")
            //editUser(userId, position,editedUser)
            dialogInterface.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        builder.show()
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
        val editButton: Button = itemView.findViewById(R.id.editButton)
        private var isUserQueued: Boolean = false

        private val isFullTextView: TextView = itemView.findViewById(R.id.IsFullTextView)
        private val hasDepartedTextView: TextView = itemView.findViewById(R.id.HasDepartedTextView)

        private fun isPointInsidePolygon(point: Pair<Double, Double>, vertices: List<Pair<Double, Double>>): Boolean {
            var isInside = false
            val x = point.first
            val y = point.second

            var j = vertices.size - 1
            for (i in vertices.indices) {
                val xi = vertices[i].first
                val yi = vertices[i].second
                val xj = vertices[j].first
                val yj = vertices[j].second

                val intersect = ((yi > y) != (yj > y)) &&
                        (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
                if (intersect) isInside = !isInside
                j = i
            }
            return isInside
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
                    editButton.visibility = View.VISIBLE

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
                            Pair(14.168251396978187, 121.24161688458322),  // Top Right
                            Pair(14.167382788504096, 121.2420138515174), // Bottom Right
                            Pair(14.167954926196032, 121.24323693882818), // Bottom Left
                            Pair(14.168984770407006, 121.24287215840216) // Top Left
                        )
                        Log.d("VERTICES", "Forestry")
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
                    Log.d("VERTICES", "${user.latitude} ${user.longitude}")
                    Log.d("VERTICES", "isInside Terminal: $vertices")
                    val isInside = isPointInsidePolygon(Pair(user.latitude, user.longitude), vertices)
                    Log.d("VERTICES", "isInside Terminal: $isInside")
                    val hasDeparted = if(isInside) "Not Departed" else "Departed"
                    isFullTextView.text = isFull
                    hasDepartedTextView.text = hasDeparted

                    queueButton.visibility = View.VISIBLE
                    isFullTextView.visibility = View.VISIBLE
                    hasDepartedTextView.visibility = View.VISIBLE
                    showDriverDetails.visibility = View.VISIBLE

                    authorizeButton.visibility = View.GONE
                    deleteButton.visibility = View.GONE
                    editButton.visibility = View.GONE


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
                    editButton.visibility = View.GONE
                    isFullTextView.visibility = View.GONE
                    hasDepartedTextView.visibility = View.GONE
                }
            }
        }
    }
}