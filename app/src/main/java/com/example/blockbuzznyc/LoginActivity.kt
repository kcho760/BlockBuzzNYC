package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blockbuzznyc.ui.theme.DarkCharcoal
import com.example.blockbuzznyc.ui.theme.Typography
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(onLoginSuccessful: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (FirebaseAuth.getInstance().currentUser != null) {
        onLoginSuccessful()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BlockBuzzNYC",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                ) // Apply background with rounded corners
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp)) // Then apply the border
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Add padding here to create space between the border and the content
            Column(
                modifier = Modifier.padding(16.dp), // This is the padding inside the Box
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                Button(onClick = { loginUser(email, password, onLoginSuccessful) }) {
                    Text("Login")
                }
                Button(onClick = { /* Navigate to sign up screen */ }) {
                    Text(
                        "Don't have an account?\nSign up!",
                        textAlign = TextAlign.Center,
                    )

                }
            }
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
