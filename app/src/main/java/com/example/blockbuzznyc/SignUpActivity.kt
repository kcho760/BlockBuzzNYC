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
import com.example.blockbuzznyc.ui.theme.DarkCharcoal
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignUpScreen(onSignUpSuccessful: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(R.string.default_web_client_id.toString())
        .build()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoal),
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
                Button(onClick = {
                    registerUser(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
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
    onSignUpSuccessful: () -> Unit,
    onSignUpFailed: (String) -> Unit
) {
    Log.d("SignUp", "Starting user registration")
    // Check if the email is valid
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

    // Create a new user with Firebase Authentication
    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign up success, update UI with the signed-in user's information
                Log.d("SignUp", "createUserWithEmail:success")
                onSignUpSuccessful()
            } else {
                // If sign up fails, display a message to the user.
                Log.w("SignUp", "createUserWithEmail:failure", task.exception)
                onSignUpFailed(task.exception?.message ?: "Authentication failed.")
            }
        }
}