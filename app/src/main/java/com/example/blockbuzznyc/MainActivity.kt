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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
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
import com.example.blockbuzznyc.model.MapPin
import com.example.blockbuzznyc.ui.theme.BlockBuzzNYCTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageHandler: ImageHandler
    private lateinit var navController: NavHostController
    private val isLoggedIn = mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    private val selectedPinLocation = mutableStateOf<LatLng?>(null)
    private val showPinInfoDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        imageHandler = ImageHandler(this, this)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
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
                MainScreen(imageHandler, googleSignInLauncher, this, navController, isLoggedIn, showPinInfoDialog)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String?, navController: NavController) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
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
    isLoggedIn: MutableState<Boolean>,
    showPinInfoDialog: MutableState<Boolean>
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val username = remember {mutableStateOf("")}
    val showUsernameDialog = remember { mutableStateOf(false) }
    val startDestination = if (isLoggedIn.value) "main" else "login"
    val selectedMapPin = remember { mutableStateOf<MapPin?>(null)}

    LaunchedEffect(key1 = isLoggedIn.value) {
        if (isLoggedIn.value) {
            // User is logged in, fetch username or show dialog if username is not set.
            currentUser?.let { user ->
                fetchUsername(user.uid) { fetchedUsername ->
                    if (fetchedUsername.isBlank()) {
                        showUsernameDialog.value = true
                    } else {
                        username.value = fetchedUsername
                    }
                }
            }
        } else {
            // User is not logged in, navigate to the login screen.
            navController.navigate("login") {
                // Clear back stack so the user can't navigate back to the main screen without logging in.
                popUpTo("main") { inclusive = true }
            }
        }
    }

    if (showUsernameDialog.value) {
        UsernameCreationDialog(
            onUsernameSet = { newUsername ->
                saveUsernameToFirestore(currentUser!!.uid, newUsername) {
                    showUsernameDialog.value = false
                    username.value = newUsername
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isLoggedIn.value) {
                Log.d("TopBar", "${isLoggedIn.value}")
                Log.d("TopBar", "TopAppBar called")
                TopAppBar(
                    title = { Text("BlockBuzzNYC - ${username.value}") }, // Include the user's email in the title
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.popBackStack()
                            navController.navigate("login")
                            isLoggedIn.value = false
                        }) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isLoggedIn.value) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = Color.White
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("profile") {
                                    inclusive = true}
                            }
                        },
                        colors = buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Profile")
                    }

                    Button(modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("main") {
                                popUpTo("main") {
                                    inclusive = true}
                            }
                        },
                        colors = buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Map")
                    }

                    Button(modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("search") {
                                popUpTo("search") {
                                    inclusive = true}
                            }
                        },
                        colors = buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Search")
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    navController = navController,
                    onLoginSuccessful = {
                        isLoggedIn.value = true
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    googleSignInLauncher = googleSignInLauncher,
                    activityContext = activityContext
                )
            }
            composable(
                "main",
                enterTransition = { slideInFromRight() },
                exitTransition = { slideOutToRight() }
            ) {
                GoogleMapComposable(
                    imageHandler,
                    navController = navController,
                    showPinInfoDialog = showPinInfoDialog,
                    selectedMapPin = selectedMapPin
                )
            }
            composable("signup") {
                SignUpScreen(navController = navController) {
                    isLoggedIn.value = true
                    navController.navigate("main") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            }

            composable(
                "profile",
                enterTransition = { slideInFromLeft() },
                exitTransition = { slideOutToRight()}
            ) {
                ProfileScreen(
                    imageHandler = imageHandler,
                    onPinSelected = { pin ->
                        Log.d("PinTracking", "Navigating to map with pin: ${pin.id}")
                        selectedMapPin.value = pin // Set the selected MapPin
                        showPinInfoDialog.value = true
                        navController.navigate("main")
                    }
                )
            }
            composable(
                "search",
                enterTransition = { slideInFromRight() },
                exitTransition = { slideOutToRight() }
            ) {
                SearchScreen(onPinSelected = { selectedPin ->
                    selectedMapPin.value = selectedPin // Set the selectedMapPin with the whole MapPin object
                    showPinInfoDialog.value = true
                    navController.navigate("main")
                })
            }
            composable("chatScreen/{pinId}/{pinTitle}") { backStackEntry ->
                val pinId = backStackEntry.arguments?.getString("pinId") ?: ""
                val pinTitle = backStackEntry.arguments?.getString("pinTitle") ?: ""
                ChatScreen(navController, pinId, pinTitle)
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

fun fetchUsername(userId: String, onUsernameFetched: (String) -> Unit) {
    val db = Firebase.firestore
    db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            val username = document.getString("username") ?: ""
            onUsernameFetched(username)
        }
        .addOnFailureListener {
            // Handle failure, maybe use a default value or an error placeholder
            onUsernameFetched("Unknown User")
        }
}
fun saveUsernameToFirestore(userId: String, username: String, onSaved: () -> Unit) {
    val userData = mapOf(
        "userId" to userId,
        "username" to username
    )

    Firebase.firestore.collection("users").document(userId).set(userData, SetOptions.merge())
        .addOnSuccessListener {
            Log.d("Firestore", "User data saved successfully")
            onSaved()
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error saving user data", e)
            // You might want to handle this error in your UI as well
        }
}

fun slideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth }, // Slide in from right
        animationSpec = tween(700)
    )
}

fun slideInFromLeft(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth }, // Slide in from right
        animationSpec = tween(700)
    )
}

fun slideOutToRight(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth }, // Slide out to right
        animationSpec = tween(700)
    )
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    BlockBuzzNYCTheme {
//        // Greeting("Android")
//    }
//}
