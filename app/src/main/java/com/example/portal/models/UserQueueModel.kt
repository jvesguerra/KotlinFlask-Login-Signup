package com.example.portal.models

import com.google.gson.annotations.SerializedName

class UserQueueModel (
    @SerializedName("queuedDriver")
    val queuedDriver: Int,

    @SerializedName("isQueued")
    val isQueued: Boolean,
)