package com.example.portal.models

import com.google.gson.annotations.SerializedName

class PetitionModel (
    @SerializedName("id")
    val id: Int,

    @SerializedName("forestryPetitionCount")
    val forestryPetitionCount: List<String>,

    @SerializedName("ruralPetitionCount")
    val ruralPetitionCount: List<String>,
)