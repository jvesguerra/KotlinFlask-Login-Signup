package com.example.portal.api

import com.example.portal.models.DriverVecLocModel
import com.example.portal.models.EditUserModel

interface OnQueueUserListener {
    fun onQueueUser(userId: Int, position: Int, vehicleId: Int)

    fun onRemoveUserQueue(userId: Int, position: Int, vehicleId: Int)

    fun editUser(userId: Int, position: Int, userModel: EditUserModel)
}