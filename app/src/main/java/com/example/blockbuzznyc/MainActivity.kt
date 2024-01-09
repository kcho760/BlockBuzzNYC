//@file:Suppress("NAME_SHADOWING")

package com.example.blockbuzznyc

import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.blockbuzznyc.model.MapPin
import com.example.blockbuzznyc.ui.theme.BlockBuzzNYCTheme
import com.example.blockbuzznyc.ui.theme.SteelBlue
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val imageHandler = ImageHandler(this, this) // Initialize ImageHandler

        setContent {
            BlockBuzzNYCTheme {
                // Call MainScreen with only the imageHandler argument
                MainScreen(imageHandler)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(imageHandler: ImageHandler) {
    val navController = rememberNavController()
    val isLoggedIn = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    // Observe the current back stack entry
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        isLoggedIn.value = false
        navController.navigate("login")
    }

    Scaffold(
        topBar = {
            // Show the TopAppBar only when not on the login route
            if (currentRoute != "login" && isLoggedIn.value) {
                TopAppBar(
                    title = { Text("BlockBuzzNYC") },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = SteelBlue,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { logoutUser() }) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController = navController, onLoginSuccessful = {
                    isLoggedIn.value = true
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
            composable("main") {
                GoogleMapComposable(imageHandler)
            }
            composable("signup") {
                SignUpScreen {
                    isLoggedIn.value = true
                    navController.navigate("main") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            }
        }
    }
}


@Composable
fun PermissionRequestUI(
    requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    permission: List<String>
) {
    // Display a message to the user explaining why these permissions are needed
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Permissions Needed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "This app requires access to your location and camera for full functionality.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
            Button(
                onClick = { requestPermissionLauncher.launch(permission.toTypedArray()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Grant Permissions")
            }
        }
    }
}




//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    BlockBuzzNYCTheme {
//        // Greeting("Android")
//    }
//}
