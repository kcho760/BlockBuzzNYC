package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.google.firebase.database.database
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
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
                Row() {
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
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Column {
                        Text(
                            text = mapPin.title,
                            style = MaterialTheme.typography.headlineSmall.copy(color = Color.Black)
                        )
                        Text(
                            text = "By ${mapPin.creatorUsername}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Row {
                            mapPin.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(50), // Circular shape
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Translucent background
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 4.dp), // Padding inside the bubble
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Black)
                                    )
                                }
                                Spacer(modifier = Modifier.size(4.dp)) // Space between bubbles
                            }
                        }
                    }
                }
            },
            text = {
                Column {
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
                        modifier = Modifier
                            .width(150.dp)
                            .fillMaxWidth() // Ensures the button fills the width but not the height
                            .padding(top = 8.dp)
                            .height(IntrinsicSize.Min) // Gives the button intrinsic height
                            .border(0.5.dp, MaterialTheme.colorScheme.onSecondary, RoundedCornerShape(50.dp)), // Example with 12.dp corner radius
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Open Chat")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(IntrinsicSize.Min) // Gives the button intrinsic height
                        .border(0.5.dp, MaterialTheme.colorScheme.onSecondary, RoundedCornerShape(50.dp)), // Example with 12.dp corner radius
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) { Text("Close") }
            },
            dismissButton = {
                if (mapPin.creatorUserId == currentUser) {
                    Button(
                        onClick = { onDelete(mapPin) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(IntrinsicSize.Min) // Gives the button intrinsic height
                            .border(0.5.dp, MaterialTheme.colorScheme.onSecondary, RoundedCornerShape(50.dp)), // Example with 12.dp corner radius
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) { Text("Delete") }
                }
            }
        )
    }
}

fun deletePin(mapPin: MapPin, currentUser: String, onSuccess: () -> Unit) {
    Log.d("PinInfoDialog", "Deleting pin with ID ${mapPin.id}")

    val db = Firebase.firestore
    val defaultPhotoUrl =
        "https://firebasestorage.googleapis.com/v0/b/blockbuzznyc.appspot.com/o/pin_images%2Fnew_york_default.jpg?alt=media&token=969960c8-7df8-4a07-8e6c-e41a419521aa"

    fun deleteChatForPin(pinId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val chatRef = Firebase.database.getReference("chats").child(pinId)
        chatRef.removeValue()
            .addOnSuccessListener {
                Log.d("PinInfoDialog", "Chat for pin $pinId successfully deleted")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("PinInfoDialog", "Error deleting chat for pin $pinId", e)
                onFailure(e)
            }
    }


    fun updateUserTotalLikes(onComplete: () -> Unit) {
        val userRef = db.collection("users").document(currentUser)
        db.runTransaction { transaction ->
            val user = transaction.get(userRef).toObject(User::class.java)
            val newTotalLikes = (user?.totalLikes ?: 0) - mapPin.likes.size
            transaction.update(userRef, "totalLikes", maxOf(newTotalLikes, 0))
        }
            .addOnSuccessListener {
                Log.d("PinInfoDialog", "User total likes updated")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("PinInfoDialog", "Error updating user total likes", e)
                // Handle failure in updating user total likes
            }
    }

    // Function to delete the image from Firebase Storage
    fun deleteImage(onComplete: () -> Unit) {
        if (mapPin.photoUrl.isNotEmpty() && mapPin.photoUrl != defaultPhotoUrl) {
            val storageRef = Firebase.storage.getReferenceFromUrl(mapPin.photoUrl)
            storageRef.delete()
                .addOnSuccessListener {
                    Log.d("PinInfoDialog", "Image successfully deleted")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("PinInfoDialog", "Error deleting image", e)
                    // Handle failure in deleting the image
                }
        } else {
            // Skip deleting the image if the URL is empty or it's the default image
            onComplete()
        }
    }

    // Function to update the lastTenPins collection
    fun updateLastTenPins(onComplete: () -> Unit) {
        db.collection("lastTenPins").get()
            .addOnSuccessListener { snapshot ->
                val pinsList = snapshot.documents.mapNotNull { it.id }
                if (mapPin.id in pinsList) {
                    db.collection("lastTenPins").document(mapPin.id).delete()
                        .addOnSuccessListener {
                            Log.d("PinInfoDialog", "Pin ID removed from lastTenPins")
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e("PinInfoDialog", "Error removing pin ID from lastTenPins", e)
                            // Handle failure in removing the pin ID from lastTenPins
                        }
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PinInfoDialog", "Error retrieving lastTenPins", e)
                // Handle failure in retrieving lastTenPins
            }
    }

    // Function to delete the pin information from Firestore
    fun deletePinInfo(onComplete: () -> Unit) {
        if (mapPin.id.isNotEmpty()) {
            db.collection("pins").document(mapPin.id).delete()
                .addOnSuccessListener {
                    Log.d("PinInfoDialog", "Pin successfully deleted")
                    onComplete() // Correctly invoking onComplete here
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
// Start the delete process by attempting to delete the image first
    deleteImage {
        deletePinInfo {
            deleteChatForPin(mapPin.id, onSuccess = {
                updateLastTenPins {
                    updateUserTotalLikes(onSuccess)
                }
            }, onFailure = { e ->
                Log.e("PinInfoDialog", "Failed to delete chat for pin ${mapPin.id}", e)
            })
        }
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