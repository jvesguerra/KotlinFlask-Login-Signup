package com.example.portal.api

import android.view.View
import com.example.portal.models.EditDriverModel

interface OnQueueUserListener {
    fun onQueueUser(position: Int, vehicleId: Int, view: View)

    fun onRemoveUserQueue(position: Int, vehicleId: Int)

    fun editUser(userId: Int, position: Int, userModel: EditDriverModel)
}