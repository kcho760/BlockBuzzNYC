package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.firebase.auth.FirebaseAuth


@Composable
fun LoginScreen(onLoginSuccessful: () -> Unit) {
    // Define state for email and password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (FirebaseAuth.getInstance().currentUser != null) {
        // User is already logged in
        onLoginSuccessful()
    }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
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
        Button(onClick = { loginUser(email, password, onLoginSuccessful) }) {
            Text("Login")
        }
        Button(onClick = { /* Navigate to sign up screen */ }) {
            Text("Sign Up")
        }
    }

}

fun loginUser(email: String, password: String, onLoginSuccessful: () -> Unit) {
    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onLoginSuccessful()
            } else {
                // Handle errors (e.g., show a message)
                Log.d("Login", "Error logging in: ${task.exception}")
            }
        }
}
