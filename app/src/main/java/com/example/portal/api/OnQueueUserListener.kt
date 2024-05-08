package com.example.portal.api

import com.example.portal.models.EditDriverModel

interface OnQueueUserListener {
    fun onQueueUser(position: Int, vehicleId: Int)

    fun onRemoveUserQueue(position: Int, vehicleId: Int)

    fun editUser(userId: Int, position: Int, userModel: EditDriverModel)
}