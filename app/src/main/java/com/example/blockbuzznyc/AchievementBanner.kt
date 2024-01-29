package com.example.blockbuzznyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.blockbuzznyc.model.Achievement
import kotlinx.coroutines.delay

@Composable
fun AchievementBanner(achievement: Achievement?, onDismiss: () -> Unit) {
    // This composable will show the banner if 'achievement' is not null
    if (achievement != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.Blue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Achievement Unlocked: ${achievement.name}",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }

        LaunchedEffect(achievement) {
            delay(3000) // Show the banner for 3 seconds
            onDismiss() // Then dismiss it
        }
    }
}
