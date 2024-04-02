package com.example.portal

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.portal.admin.AdminHome
import com.example.portal.models.DriverVehicle
import com.google.gson.Gson

class MyAdapter(private val context: AdminHome, private var items: MutableList<DriverVehicle>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.deleteButton.setOnClickListener {
            // Call deleteItem function when delete button is clicked
            deleteItem(item.userId, position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

//    fun deleteItem(position: Int) {
//        items.removeAt(position)
//        notifyItemRemoved(position)
//        notifyItemRangeChanged(position, itemCount)
//    }

    fun removeItemAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateData(newItems: List<DriverVehicle>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    private fun deleteItem(itemId: Int, position: Int) {
        Log.d("ItemID", "Item ID: $itemId") // Print item ID to logcat
        (context as? AdminHome)?.deleteItem(itemId, position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

//        init {
//            deleteButton.setOnClickListener {
//                // Call deleteItem when the delete button is clicked
//                removeItemAt(adapterPosition)
//            }
//        }

        fun bind(user: DriverVehicle) {
            val fullname = "${user.firstName} ${user.lastName}"
            itemView.findViewById<TextView>(R.id.userIdTextView).text = user.userId.toString()
            itemView.findViewById<TextView>(R.id.itemNameTextView).text = fullname
            itemView.findViewById<TextView>(R.id.RouteTextView).text = user.plateNumber
            itemView.findViewById<TextView>(R.id.PlateNumberTextView).text = user.route
        }
    }
}