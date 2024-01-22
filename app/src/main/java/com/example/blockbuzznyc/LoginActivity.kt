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
            .requestIdToken("15720236856-0q14kp2dv3hpvt6go2o831sobep98b08.apps.googleusercontent.com")
            .requestEmail()
            .build()


        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null, // Decorative image
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Fill the screen while preserving the aspect ratio
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
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
                    Button(onClick = {
                        loginUser(email, password, onLoginSuccessful, onLoginFailed = { error ->
                            errorMessage = error // Update the error message
                        })
                    }) {
                        Text("Login")
                    }
                    Button(onClick = {
                        loginUser(
                            "dev@gmail.com",
                            "password",
                            onLoginSuccessful,
                            onLoginFailed = { error ->
                                errorMessage = error // Update the error message
                            })
                    }) {
                        Text("Dev Login")
                    }
                    Button(onClick = {
                        googleSignIn(
                            context = context,
                            onLoginSuccessful = onLoginSuccessful,
                            onLoginFailed = { error -> errorMessage = error },
                            googleSignInLauncher = googleSignInLauncher
                        )
                    }) {
                        Text("Sign in with Google")
                    }

                    Button(onClick = { navController.navigate("signup") }) {
                        Text("Don't have an account?\nSign up!", textAlign = TextAlign.Center)
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
