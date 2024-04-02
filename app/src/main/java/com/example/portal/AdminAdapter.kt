package com.example.portal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.admin.AdminHome
import com.example.portal.models.DriverVehicle

class AdminAdapter(private val context: AdminHome, private var items: MutableList<DriverVehicle>) : RecyclerView.Adapter<AdminAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.deleteButton.setOnClickListener {
            deleteUser(item.userId, position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun removeItemAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateData(newItems: List<DriverVehicle>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    private fun deleteUser(userId: Int, position: Int) {
        (context as? AdminHome)?.deleteItem(userId, position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(user: DriverVehicle) {
            val fullname = "${user.firstName} ${user.lastName}"
            itemView.findViewById<TextView>(R.id.userIdTextView).text = user.userId.toString()
            itemView.findViewById<TextView>(R.id.itemNameTextView).text = fullname
            itemView.findViewById<TextView>(R.id.RouteTextView).text = user.plateNumber
            itemView.findViewById<TextView>(R.id.PlateNumberTextView).text = user.route
        }
    }
}