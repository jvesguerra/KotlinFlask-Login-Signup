package com.example.portal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.models.DriverVehicleModel
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.example.portal.api.OnDeleteUserListener
import com.example.portal.api.RetrofitInstance
import com.example.portal.api.UserServe
import retrofit2.Response
import retrofit2.Call
import retrofit2.Callback

class Adapter(
    private val onDeleteUserListener: OnDeleteUserListener?,
    private val context: Context,
    private val contextType: ContextType,
    private var items: MutableList<DriverVehicleModel>
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
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun removeItemAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateData(newItems: List<DriverVehicleModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun deleteUser(userId: Int, position: Int) {
        onDeleteUserListener?.onDeleteUser(userId, position)
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

    private fun showDriverDetailsDialog(user: DriverVehicleModel) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_driver_detail, null)

        dialogView.findViewById<TextView>(R.id.fullNameTextView).text = "${user.firstName} ${user.lastName}"
        dialogView.findViewById<TextView>(R.id.userIdTextView).text = "Contact Number: ${user.contactNumber}"
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
        private val queueButton: Button = itemView.findViewById(R.id.queueButton)
        private val isFullTextView: TextView = itemView.findViewById(R.id.IsFullTextView)
        private val hasDepartedTextView: TextView = itemView.findViewById(R.id.HasDepartedTextView)

        fun bind(user: DriverVehicleModel) {
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
                    val hasDeparted = if(user.hasDeparted) "Departed" else "Not Departed"
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