package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.blockbuzznyc.model.MapPin
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@Composable
fun PinInfoDialog(
    mapPin: MapPin?,
    currentUser: String,
    onDismiss: () -> Unit,
    onDelete: (MapPin) -> Unit,
    onLikeToggle: (MapPin) -> Unit,
    onChatButtonClick: (MapPin) -> Unit
) {
    var creatorProfilePictureUrl by remember { mutableStateOf<String?>(null) }

    if (mapPin != null) {
        LaunchedEffect(key1 = mapPin.creatorUserId) {
            val user = getUserProfile(mapPin.creatorUserId)
            creatorProfilePictureUrl = user?.profilePictureUrl
        }

        val isLiked = mapPin.likes.contains(currentUser)
        val likeButtonText = if (isLiked) "Unlike" else "Like"
        val likesCount = mapPin.likes.size

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row {
                    if (creatorProfilePictureUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = creatorProfilePictureUrl),
                            contentDescription = "Creator Profile Picture",
                            modifier = Modifier
                                .size(40.dp) // Set the size of the image
                                .clip(CircleShape) // Clip the image to a circle
                                .fillMaxSize(), // This will make sure the image fills the circle
                            contentScale = ContentScale.Crop // This will crop the image to fit the circle while maintaining the aspect ratio
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = mapPin.title,
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.Black)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Row {
                        mapPin.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(50), // Circular shape
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Translucent background
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp) // Padding inside the bubble
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(50)), // Border to create bubble effect
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Black)
                                )
                            }
                            Spacer(modifier = Modifier.size(4.dp)) // Space between bubbles
                        }
                    }
                }
            },
            text = {
                Column {

                    Text(
                        text = "By ${mapPin.creatorUsername}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    Text(
                        text = mapPin.description,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    Image(
                        painter = rememberAsyncImagePainter(model = mapPin.photoUrl),
                        contentDescription = "Pin Image",
                        modifier = Modifier.size(100.dp)
                    )
                    Text(
                        text = "Likes: $likesCount",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    // Add Like button here within the content
                    if (mapPin.creatorUserId != currentUser) {
                        Button(onClick = { onLikeToggle(mapPin) }) {
                            Text(likeButtonText)
                        }
                    }
                    Button(
                        onClick = { onChatButtonClick(mapPin) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Open Chat")
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) { Text("Close") }
            },
            dismissButton = {
                if (mapPin.creatorUserId == currentUser) {
                    Button(onClick = { onDelete(mapPin) }) { Text("Delete") }
                }
            }
        )
    }
}

fun deletePin(mapPin: MapPin, onSuccess: () -> Unit) {
    Log.d("PinInfoDialog", "Deleting pin with ID $mapPin")
    if (mapPin.id.isNotEmpty()) { // Check if the ID is not empty
        val db = Firebase.firestore
        db.collection("pins").document(mapPin.id).delete() // Ensure mapPin.id is a valid document ID
            .addOnSuccessListener {
                Log.d("PinInfoDialog", "Pin successfully deleted")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("PinInfoDialog", "Error deleting pin", e)
                // Handle failure
            }
    } else {
        Log.e("PinInfoDialog", "Error: Pin ID is empty")
        // Handle case where pin ID is empty
    }
}
fun toggleLikeOnPin(mapPin: MapPin, currentUser: String, onUpdated: (MapPin) -> Unit) {
    val db = Firebase.firestore
    val pinRef = db.collection("pins").document(mapPin.id)
    val userRef = db.collection("users").document(mapPin.creatorUserId) // Reference to the pin creator's user document
    if (mapPin.creatorUserId == currentUser) {
        return // Do nothing if the currentUser is the creator
    }

    val newLikes = if (currentUser in mapPin.likes) {
        mapPin.likes - currentUser // Remove like
    } else {
        mapPin.likes + currentUser // Add like
    }

    // Optimistically update the UI
    val updatedMapPin = mapPin.copy(likes = newLikes)
    onUpdated(updatedMapPin)

    db.runTransaction { transaction ->
        val currentLikes = transaction.get(pinRef).toObject(MapPin::class.java)?.likes ?: listOf()
        val updatedLikes = if (currentUser in currentLikes) {
            currentLikes - currentUser
        } else {
            currentLikes + currentUser
        }

        val userSnapshot = transaction.get(userRef)
        var totalLikes = userSnapshot.getLong("totalLikes") ?: 0

// Check if the currentUser is in the newLikes list to determine if we're adding or removing a like
        if (currentUser in newLikes) {
            // Adding a like
            totalLikes++
        } else {
            // Removing a like
            if (totalLikes > 0) totalLikes--
        }


        // Update the pin likes and user's total likes
        transaction.update(pinRef, "likes", updatedLikes)
        transaction.update(userRef, "totalLikes", totalLikes)
    }.addOnSuccessListener {
        Log.d("MapPin", "Pin like toggled and totalLikes updated successfully.")
    }.addOnFailureListener {
        Log.e("MapPin", "Error toggling pin like or updating totalLikes.", it)
        // Optionally: Handle failure (e.g., rollback optimistic UI update)
    }
}

suspend fun getUserProfile(userId: String): User? {
    val db = Firebase.firestore
    return db.collection("users").document(userId).get().await()
        .toObject(User::class.java)
}