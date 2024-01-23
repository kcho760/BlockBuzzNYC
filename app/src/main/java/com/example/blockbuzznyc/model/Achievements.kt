package com.example.blockbuzznyc.model


import com.google.firebase.Timestamp

data class Achievement(
    val id: String = "", // Unique identifier for the achievement
    val name: String = "", // Display name of the achievement
    val description: String= "", // Description of how to earn the achievement
    val earned: Boolean = false, // Whether the user has earned this achievement
    val timestamp: Timestamp = Timestamp.now() // When the achievement was earned
)

