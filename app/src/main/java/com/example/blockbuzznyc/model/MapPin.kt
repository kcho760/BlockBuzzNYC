package com.example.blockbuzznyc.model

data class MapPin(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUrl: String = "",
    var creatorUserId: String = "",
    val creatorUsername: String = "",
    val likes: List<String> = listOf(),
    val tags: List<String> = listOf()
)
