package com.example.blockbuzznyc

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
import androidx.compose.ui.graphics.Color

@Composable
fun UsernameCreationDialog(onUsernameSet: (String) -> Unit) {
    var newUsername by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { /* Handle dismiss */ },
        title = { Text("Set Username") },
        text = {
            Column {
                TextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Username") }
                )
                errorMessage?.let {
                    Text(it, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newUsername.isNotBlank()) {
                        isUsernameAvailable(newUsername) { isAvailable ->
                            if (isAvailable) {
                                onUsernameSet(newUsername)
                            } else {
                                errorMessage = "Username is already taken"
                            }
                        }
                    } else {
                        errorMessage = "Username cannot be empty"
                    }
                }
            ) {
                Text("Confirm")
            }
        }
    )
}
