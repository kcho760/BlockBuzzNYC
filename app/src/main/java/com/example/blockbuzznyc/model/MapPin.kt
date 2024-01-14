package com.example.blockbuzznyc.model

data class MapPin(
    val title: String = "", // Provide default values
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUrl: String = "",
    val creatorUserId: String = "",
    val creatorUsername: String = ""
)
