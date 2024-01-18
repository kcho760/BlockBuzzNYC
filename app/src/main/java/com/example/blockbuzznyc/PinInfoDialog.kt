package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.blockbuzznyc.model.MapPin
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun PinInfoDialog(mapPin: MapPin?, currentUser: String, onDismiss: () -> Unit, onDelete: (MapPin) -> Unit) {
    if (mapPin != null) {
        // Log the IDs for debugging
        Log.d("PinInfoDialog", "Current user: $currentUser, Pin creator: ${mapPin.creatorUserId}")

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = mapPin.title,
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color.Black)
                )
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
                    mapPin.photoUrl?.let { url ->
                        Image(
                            painter = rememberAsyncImagePainter(model = url),
                            contentDescription = "Pin Image",
                            modifier = Modifier.size(100.dp)
                        )
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
