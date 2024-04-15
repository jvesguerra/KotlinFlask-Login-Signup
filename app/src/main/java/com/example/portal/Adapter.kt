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

        holder.queueButton.setOnClickListener {
            showQueueConfirmationDialog(item.userId, position, item.vehicleId)
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
        val retrofitService: UserServe = RetrofitInstance.getRetrofitInstance()
            .create(UserServe::class.java)
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

        private val isFullTextView: TextView = itemView.findViewById(R.id.IsFullTextView)
        private val hasDepartedTextView: TextView = itemView.findViewById(R.id.HasDepartedTextView)

        // determine if vehicle is in terminal
        fun isPointInPolygon(point: Pair<Double, Double>, vertices: List<Pair<Double, Double>>): Boolean {
            val (px, py) = point
            var isInside = false
            var i = 0
            var j = vertices.size - 1

            while (i < vertices.size) {
                val (x_i, y_i) = vertices[i]
                val (x_j, y_j) = vertices[j]

                if ((y_i > py) != (y_j > py) &&
                    (px < (x_j - x_i) * (py - y_i) / (y_j - y_i) + x_i)) {
                    isInside = !isInside
                }

                j = i++
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

                    authorizeButton.visibility = View.GONE
                    queueButton.visibility = View.GONE

                    isFullTextView.visibility = View.GONE
                    hasDepartedTextView.visibility = View.GONE
                }
                ContextType.USER_HOME2 -> {
                    val isFull = if(user.isFull) "Full" else "Not Full"



                    val vertices = if (user.route == "Forestry") {
                        // Vertices for forestry
                        listOf(
                            Pair(14.168132, 121.242189), // Top Left
                            Pair(14.167896, 121.242343), // Bottom Left
                            Pair(14.168105, 121.243113), // Bottom Right
                            Pair(14.168517, 121.243002)  // Top Right
                        )
                    } else {
                        // Vertices for other route
                        listOf(
                            Pair(14.169, 121.242), // Top Left
                            Pair(14.167, 121.242), // Bottom Left
                            Pair(14.169, 121.243), // Bottom Right
                            Pair(14.167, 121.243)  // Top Right
                        )
                    }
                    // check gps coordinates here
                    val deviceLocation = Pair(user.latitude.toDouble(), user.latitude.toDouble())
                    val isInside = isPointInPolygon(deviceLocation, vertices)
                    val hasDeparted = if(isInside) "Not Departed" else "Departed"
                    isFullTextView.text = isFull
                    hasDepartedTextView.text = hasDeparted

                    queueButton.visibility = View.VISIBLE
                    isFullTextView.visibility = View.VISIBLE
                    hasDepartedTextView.visibility = View.VISIBLE

                    authorizeButton.visibility = View.GONE
                    deleteButton.visibility = View.GONE
                    showDriverDetails.visibility = View.VISIBLE
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