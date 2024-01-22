package com.example.blockbuzznyc

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun SignUpScreen(onSignUpSuccessful: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BlockBuzzNYC - Sign Up",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") }
                )
                Button(onClick = {
                    registerUser(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        username = username,
                        onSignUpSuccessful = onSignUpSuccessful,
                        onSignUpFailed = { error ->
                            errorMessage = error
                        }
                    )
                }) {
                    Text("Sign Up")
                }


                // Display error message
                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }
            }
        }
    }
}

private fun registerUser(
    email: String,
    password: String,
    confirmPassword: String,
    username: String,
    onSignUpSuccessful: () -> Unit,
    onSignUpFailed: (String) -> Unit
) {
    // Check if the email is valid
    if (username.isBlank()) {
        onSignUpFailed("Username cannot be empty")
        return
    }
    if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        onSignUpFailed("Please enter a valid email address")
        return
    }

    // Check if the passwords match
    if (password != confirmPassword) {
        onSignUpFailed("Passwords do not match")
        return
    }

    // Check if the password is valid (you can add more complex password rules here)
    if (password.length < 6) {
        onSignUpFailed("Password must be at least 6 characters")
        return
    }
    isUsernameAvailable(username) { isAvailable -> //create function once model is set up
        if (!isAvailable) {
            onSignUpFailed("Username is already taken")
            return@isUsernameAvailable
        }

        // Create a new user with Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    // Save the userId and username to Firestore
                    saveUsernameToFirestore(userId, username, onSignUpSuccessful, onSignUpFailed)
                    // Sign up success, update UI with the signed-in user's information
                    onSignUpSuccessful()
                } else {
                    // If sign up fails, display a message to the user.
                    onSignUpFailed(task.exception?.message ?: "Authentication failed.")
                }
            }
    }
}

fun saveUsernameToFirestore(
    userId: String,
    username: String,
    onSignUpSuccessful: () -> Unit,
    onSignUpFailed: (String) -> Unit
) {
    val user = hashMapOf(
        "userId" to userId,
        "username" to username
    )

    Firebase.firestore.collection("users").document(userId).set(user)
        .addOnSuccessListener {
            Log.d("Firestore", "User data saved successfully")
            onSignUpSuccessful()
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error saving user data", e)
            onSignUpFailed("Failed to save user data")
        }
}

fun isUsernameAvailable(username: String, callback: (Boolean) -> Unit) {
    val db = Firebase.firestore

    db.collection("users")
        .whereEqualTo("username", username)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                // Username is available
                callback(true)
            } else {
                // Username is taken
                callback(false)
            }
        }
        .addOnFailureListener {
            // Handle any errors, for simplicity, assume username is not available
            callback(false)
        }
}
