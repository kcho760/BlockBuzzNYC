//@file:Suppress("NAME_SHADOWING")

package com.example.blockbuzznyc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.blockbuzznyc.ui.theme.BlockBuzzNYCTheme
import com.example.blockbuzznyc.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blockbuzznyc.model.MapPin
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            BlockBuzzNYCTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val isLoggedIn = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    LaunchedEffect(isLoggedIn.value) {
        if (isLoggedIn.value) {
            navController.navigate("main") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        } else {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }

    }

    NavHost(navController = navController, startDestination = if (isLoggedIn.value) "main" else "login") {
        composable("login") {
            LoginScreen(navController, onLoginSuccessful = {
                isLoggedIn.value = true
            })
        }
        composable("main") {
            GoogleMapComposable(onLogout = { isLoggedIn.value = false })
        }
        composable("signup") {
            SignUpScreen(onSignUpSuccessful = { isLoggedIn.value = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapComposable(onLogout: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("") }
    var selectedLatLng: LatLng? by remember { mutableStateOf(null) }
    var mapViewInstance: MapView? by remember { mutableStateOf(null) }

    // State to track permission status
    var hasFineLocationPermission by remember { mutableStateOf(false) }

    val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val context = LocalContext.current

    // Check and update permission status
    LaunchedEffect(Unit) {
        hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            fineLocationPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasFineLocationPermission = isGranted
    }

    LaunchedEffect(hasFineLocationPermission) {
        if (!hasFineLocationPermission) {
            requestPermissionLauncher.launch(fineLocationPermission)
        }
    }

    if (!hasFineLocationPermission) {
        PermissionRequestUI(requestPermissionLauncher, fineLocationPermission)
        return
    }
    fun logoutUser(onLogout: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        onLogout()
    }
    fun savePinToFirestore(mapPin: MapPin) {
        val db = Firebase.firestore
        db.collection("pins")
            .add(mapPin)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BlockBuzzNYC") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = SteelBlue, // Set the background color here
                    titleContentColor = Color.White // Set the title text color here
                ),
                actions = {
                    IconButton(onClick = { logoutUser(onLogout) }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AndroidView(
                factory = { context ->
                    MapView(context).also { mapView ->
                        mapViewInstance = mapView
                        mapView.onCreate(null)
                        mapView.getMapAsync { googleMap ->
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    location?.let {
                                        val currentLatLng = LatLng(it.latitude, it.longitude)
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                                    } ?: run {
                                        val defaultLatLng = LatLng(40.7128, -74.0060) // New York City coordinates
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 17f))
                                    }
                                }
                            fetchAndDisplayPins(googleMap)
                            googleMap.setOnMapLongClickListener { latLng -> // What happens when the map is long clicked
                                selectedLatLng = latLng
                                showDialog = true
                            }
                        }
                    }
                },
                update = { mapView ->
                    mapView.onResume()
                }
            )}
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(
                    text = "Add a title",
                    color = Color.Black
                )
            },
            text = {
                TextField(
                    value = pinTitle,
                    onValueChange = { pinTitle = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Logic to dismiss the dialog and save the pin information
                        showDialog = false
                        selectedLatLng?.let { latLng ->
                            val mapPin = MapPin(
                                title = pinTitle,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude
                            )
                            // This function would handle the Firebase Firestore save operation
                            savePinToFirestore(mapPin)
                            // You might also want to add the marker to the map view here
                            mapViewInstance?.getMapAsync { googleMap ->
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title(pinTitle)
                                )
                            }
                        }
                    },
                ) {
                    Text(
                        text = "Confirm",
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}


@Composable
fun PermissionRequestUI(
    requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    permission: String
) {
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
                text = "Location Permission Needed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "This app requires location permission to function properly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
            Button(
                onClick = { requestPermissionLauncher.launch(permission) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Grant Permission")
            }
        }
    }
}

fun fetchAndDisplayPins(googleMap: GoogleMap) {
    val db = Firebase.firestore
    db.collection("pins")
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val mapPin = document.toObject(MapPin::class.java)
                val location = LatLng(mapPin.latitude, mapPin.longitude)
                googleMap.addMarker(MarkerOptions().position(location).title(mapPin.title))
            }
        }
        .addOnFailureListener { exception ->
            Log.w("Firestore", "Error getting documents: ", exception)
        }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlockBuzzNYCTheme {
        // Greeting("Android")
    }
}
