package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blockbuzznyc.model.MapPin
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinEditDialog(
    mapPin: MapPin,
    onUpdate: (MapPin) -> Unit,
    onDismiss: () -> Unit,
    availableTags: List<String> = listOf("Food", "Art", "Other", "Nature", "Entertainment")
) {
    var title by remember { mutableStateOf(mapPin.title) }
    var description by remember { mutableStateOf(mapPin.description) }
    var selectedTags by remember { mutableStateOf(mapPin.tags) }

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
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    availableTags.forEach { tag ->
                        TagButton(tag, selectedTags.contains(tag)) { isSelected ->
                            selectedTags = if (isSelected) {
                                selectedTags + tag
                            } else {
                                selectedTags - tag
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedPin = mapPin.copy(
                        title = title,
                        description = description,
                        tags = selectedTags
                    )
                    updatePin(updatedPin,
                        onSuccess = {
                            onUpdate(it)  // Use 'onUpdate' lambda to propagate the updated pin upward.
                            onDismiss()
                        },
                        onFailure = { e ->
                            Log.e("PinEditDialog", "Error updating pin: ${e.localizedMessage}", e)
                            onDismiss() // Optionally dismiss the dialog
                        }
                    )
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
fun updatePin(mapPin: MapPin, onSuccess: (MapPin) -> Unit, onFailure: (Exception) -> Unit) {
    val db = Firebase.firestore
    val pinRef = db.collection("pins").document(mapPin.id)

    db.runTransaction { transaction ->
        transaction.set(pinRef, mapPin)
    }.addOnSuccessListener {
        Log.d("MapPin", "Pin updated successfully.")
        onSuccess(mapPin) // Invoke the success callback
    }.addOnFailureListener { e ->
        Log.e("MapPin", "Error updating pin.", e)
        onFailure(e) // Invoke the failure callback
    }
}
