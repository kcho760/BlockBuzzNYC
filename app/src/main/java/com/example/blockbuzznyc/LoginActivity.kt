package com.example.blockbuzznyc

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccessful: () -> Unit,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    activityContext: Context
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (FirebaseAuth.getInstance().currentUser != null) {
        onLoginSuccessful()
    }

    fun googleSignIn(
            context: Context,
            onLoginSuccessful: () -> Unit,
            onLoginFailed: (String) -> Unit,
            googleSignInLauncher: ActivityResultLauncher<Intent>
    ) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("15720236856-cgkg47sis7b2g6k6a24umdgruku256le.apps.googleusercontent.com")
                .requestEmail()
                .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        val signInIntent = googleSignInClient.signInIntent
        try {
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.d("SignUp", "Failed to launch sign-in Intent: ${e.message}")
            onLoginFailed("Failed to initiate sign-in: ${e.localizedMessage}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null, // Decorative image
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Fill the screen while preserving the aspect ratio
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
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
                        MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(12.dp)
                    ) // Apply background with rounded corners
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp)) // Then apply the border
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), // This is the padding inside the Box
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TextField(
                        modifier = Modifier.border(1.dp, Color.Black),
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        textStyle = TextStyle(color = Color.Black),
                        )
                    TextField(
                        modifier = Modifier.border(1.dp, Color.Black),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color.Black),
                        )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            loginUser(email, password, onLoginSuccessful, onLoginFailed = { error ->
                                errorMessage = error // Update the error message
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Login", color = MaterialTheme.colorScheme.onTertiary)
                    }
                    Button(
                        onClick = {
                            loginUser(
                                "dev@gmail.com",
                                "password",
                                onLoginSuccessful,
                                onLoginFailed = { error ->
                                    errorMessage = error // Update the error message
                                })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Dev Login", color = MaterialTheme.colorScheme.onTertiary)
                    }
                    Button(
                        onClick = {
                            googleSignIn(
                                context = context,
                                onLoginSuccessful = onLoginSuccessful,
                                onLoginFailed = { error -> errorMessage = error },
                                googleSignInLauncher = googleSignInLauncher
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Sign in with Google", color = MaterialTheme.colorScheme.onTertiary)
                    }

                    Button(
                        onClick = { navController.navigate("signup") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Don't have an account?\nSign up!", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onTertiary)
                    }
                }
            }
        }
    }
}



fun loginUser(email: String, password: String, onLoginSuccessful: () -> Unit, onLoginFailed: (String) -> Unit) {
    if (email.isBlank() || password.isBlank()) {
        Log.d("SignUp", "Email or password is blank")
        onLoginFailed("Email and password must not be empty")
        return
    }

    // Firebase Authentication call
    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SignUp", "Login successful for user: $email")
                onLoginSuccessful()
            } else {
                Log.d("SignUp", "Error logging in: ${task.exception}")
                onLoginFailed(task.exception?.message ?: "Login failed")
            }
        }
}
