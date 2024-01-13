//@file:Suppress("NAME_SHADOWING")

package com.example.blockbuzznyc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blockbuzznyc.ui.theme.BlockBuzzNYCTheme
import com.example.blockbuzznyc.ui.theme.SteelBlue
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageHandler: ImageHandler
    private lateinit var navController: NavHostController
    private val isLoggedIn = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        imageHandler = ImageHandler(this, this)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("SignUp", "Google Sign-In result received")
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("SignUp", "Result OK, processing sign-in")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d("SignUp", "Google Sign-In successful, ID Token: ${account.idToken}")
                    firebaseAuthWithGoogle(account.idToken, navController)
                } catch (e: ApiException) {
                    Log.d("SignUp", "Google Sign-In failed with code: ${e.statusCode}")
                }
            } else {
                Log.d("SignUp", "Sign-In result not OK")
            }
        }

        setContent {
            navController = rememberNavController()
            val isLoggedIn = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
            BlockBuzzNYCTheme {
                MainScreen(imageHandler, googleSignInLauncher, this, navController, isLoggedIn)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String?, navController: NavController) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Log.d("SignUp", "NavController Loaded $navController")
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SignUp", "signInWithCredential:success")
                    isLoggedIn.value = true
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Log.w("SignUp", "signInWithCredential:failure", task.exception)
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    imageHandler: ImageHandler,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    activityContext: Context,
    navController: NavHostController,
    isLoggedIn: MutableState<Boolean>
) {
    LaunchedEffect(isLoggedIn.value) {
        if (isLoggedIn.value) {
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
        }
    }


    Scaffold(
        topBar = {
            if (isLoggedIn.value) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userEmail = currentUser?.email ?: "" // Get the user's email or use an empty string if not available
                TopAppBar(
                    title = { Text("BlockBuzzNYC - $userEmail") }, // Include the user's email in the title
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = SteelBlue,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            isLoggedIn.value = false
                            navController.navigate("login")
                        }) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    navController = navController,
                    onLoginSuccessful = { /* ... */ },
                    googleSignInLauncher = googleSignInLauncher,
                    activityContext = activityContext // Pass the activity context
                )
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
