package com.example.portal.api

interface OnQueueUserListener {
    fun onQueueUser(userId: Int, position: Int, vehicleId: Int)
}