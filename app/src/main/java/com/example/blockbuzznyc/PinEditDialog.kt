package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.blockbuzznyc.model.MapPin
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun PinEditDialog(
    mapPin: MapPin,
    onUpdate: (MapPin) -> Unit,
    onDismiss: () -> Unit
) {
    // States for editing fields
    var title by remember { mutableStateOf(mapPin.title) }
    var description by remember { mutableStateOf(mapPin.description) }
    var tags by remember { mutableStateOf(mapPin.tags.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Pin")
        },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                TextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") }
                )
                // TODO: Add UI for image editing
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedPin = mapPin.copy(
                        title = title,
                        description = description,
                        tags = tags.split(",").map(String::trim)
                    )
                    onUpdate(updatedPin)
                    onDismiss()
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Function to update the pin in Firebase Firestore
fun updatePin(mapPin: MapPin) {
    val db = Firebase.firestore
    val pinRef = db.collection("pins").document(mapPin.id)

    db.runTransaction { transaction ->
        transaction.set(pinRef, mapPin)
    }.addOnSuccessListener {
        Log.d("MapPin", "Pin updated successfully.")
    }.addOnFailureListener {
        Log.e("MapPin", "Error updating pin.", it)
    }
}

// This function should be tied to the edit button click
fun onEditButtonClick(mapPin: MapPin) {
    // Trigger the dialog to edit the pin
}
