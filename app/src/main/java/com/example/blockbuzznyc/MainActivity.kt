//@file:Suppress("NAME_SHADOWING")

package com.example.blockbuzznyc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.blockbuzznyc.model.MapPin
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import androidx.compose.material.icons.filled.MyLocation


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val imageHandler = ImageHandler(this, this) // Instantiate ImageHandler
        setContent {
            BlockBuzzNYCTheme {
                AppNavigation(imageHandler) // Pass ImageHandler to AppNavigation
            }
        }
    }
}

@Composable
fun AppNavigation(imageHandler: ImageHandler) {
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
            GoogleMapComposable(imageHandler, onLogout = { isLoggedIn.value = false })
        }
        composable("signup") {
            SignUpScreen(onSignUpSuccessful = { isLoggedIn.value = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapComposable(imageHandler: ImageHandler ,onLogout: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("") }
    var pinDescription by remember { mutableStateOf("") }
    var selectedLatLng: LatLng? by remember { mutableStateOf(null) }
    var mapViewInstance: MapView? by remember { mutableStateOf(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasPermissions by remember { mutableStateOf(false) }
    val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var showPinInfoDialog by remember { mutableStateOf(false) }
    var selectedMapPin: MapPin? by remember { mutableStateOf(null) }
    var googleMapInstance: GoogleMap? by remember { mutableStateOf(null) }
    var currentLatLngInstance: LatLng? by remember { mutableStateOf(null) }


    // Check and update permission status
    LaunchedEffect(Unit) {
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val requestMultiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
    }

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    if (!hasPermissions) {
        PermissionRequestUI(requestMultiplePermissionsLauncher, permissions)
        return
    }
    fun logoutUser(onLogout: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        onLogout()
    }

    fun setupGoogleMap(googleMap: GoogleMap, context: Context) {
        // Set custom info window adapter
        val infoWindowAdapter = CustomInfoWindowAdapter(context)
        googleMap.setInfoWindowAdapter(infoWindowAdapter)

        // Set a listener for marker click

        googleMap.setOnMarkerClickListener { marker ->
            val pinInfo = marker.tag as? PinInfo
            pinInfo?.let {
                selectedMapPin = marker.title?.let { it1 ->
                    MapPin(
                        title = it1,
                        description = it.description,
                        latitude = marker.position.latitude,
                        longitude = marker.position.longitude,
                        photoUrl = it.photoUrl
                    )
                }
                showPinInfoDialog = true
            }
            true  // Return true to indicate that we have handled the event
        }
    }

    fun recenterMap(googleMap: GoogleMap) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BlockBuzzNYC") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = SteelBlue,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { logoutUser(onLogout) }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column {
                AndroidView(
                    factory = { context ->
                        MapView(context).also { mapView ->
                            mapViewInstance = mapView
                            mapView.onCreate(null)
                            mapView.getMapAsync { googleMap ->
                                googleMapInstance = googleMap
                                setupGoogleMap(googleMap, context)
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        val currentLatLng = LatLng(it.latitude, it.longitude)
                                        currentLatLngInstance = currentLatLng
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                                        fetchAndDisplayPins(googleMap, currentLatLng) // Pass currentLatLng to the function
                                    } ?: run {
                                        val defaultLatLng = LatLng(40.7128, -74.0060) // Default to New York City coordinates
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 17f))
                                        fetchAndDisplayPins(googleMap, defaultLatLng) // Pass defaultLatLng to the function
                                    }
                                }
                                googleMap.setOnMapLongClickListener { latLng ->
                                    selectedLatLng = latLng
                                    showDialog = true
                                }
                            }
                        }
                    },
                    update = { mapView ->
                        mapView.onResume()
                    }
                )
            }

            FloatingActionButton(
                onClick = { mapViewInstance?.getMapAsync { googleMap -> recenterMap(googleMap) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Recenter")
            }
        }
    }
    if (showPinInfoDialog) {
        PinInfoDialog(mapPin = selectedMapPin) {
            showPinInfoDialog = false
            selectedMapPin = null
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create a Pin") },
            text = {
                Column {
                    TextField(
                        value = pinTitle,
                        onValueChange = { pinTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    TextField(
                        value = pinDescription,
                        onValueChange = { pinDescription = it },
                        label = { Text("Description") },
                        singleLine = true
                    )
                    Button(onClick = {
                        imageHandler.takePicture { uri ->
                            imageUri = uri
                        }
                    }) {
                        Text(text = "Take Photo")
                    }
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = "Captured Image",
                            modifier = Modifier.size(100.dp), // Set a specific size for the image
                            contentScale = ContentScale.Fit // Adjust the scaling to fit the size while maintaining the aspect ratio
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        selectedLatLng?.let { latLng ->
                            val mapPin = MapPin(
                                title = pinTitle,
                                description = pinDescription,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude,
                                photoUrl = ""
                            )
                            imageUri?.let { uri ->
                                // Use googleMapInstance and currentLatLngInstance here
                                googleMapInstance?.let { googleMap ->
                                    currentLatLngInstance?.let { currentLatLng ->
                                        confirmAndCreatePin(mapPin, uri, googleMap, currentLatLng) { success ->
                                            if (success) {
                                                Log.d("MapPin", "Pin created successfully")
                                                // Refresh pins here if needed
                                            } else {
                                                Log.d("MapPin", "Pin creation failed")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

fun savePinToFirestore(mapPin: MapPin): Task<DocumentReference> {
    val db = Firebase.firestore
    return db.collection("pins").add(mapPin)
        .addOnSuccessListener {
            Log.d("Firestore", "Pin saved successfully with ID: ${it.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error saving pin to Firestore", e)
        }
}


fun confirmAndCreatePin(mapPin: MapPin, imageUri: Uri, googleMap: GoogleMap, currentLatLng: LatLng, onComplete: (Boolean) -> Unit) {
    val storageRef = Firebase.storage.reference
    val imageRef = storageRef.child("pin_images/${imageUri.lastPathSegment}")
    val uploadTask = imageRef.putFile(imageUri)

    uploadTask.addOnSuccessListener { taskSnapshot ->
        Log.d("MapPin", "confirmAndCreatePin called with URI: $imageUri")
        taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
            val newMapPin = mapPin.copy(photoUrl = downloadUri.toString())
            savePinToFirestore(newMapPin).addOnSuccessListener {
                fetchAndDisplayPins(googleMap, currentLatLng)
                Log.d("MapPin", "Pin saved successfully")
                onComplete(true)
            }.addOnFailureListener { e ->
                Log.e("MapPin", "Error saving pin to Firestore", e)
                onComplete(false)
            }
        }.addOnFailureListener { e ->
            Log.e("MapPin", "Error getting download URL", e)
            onComplete(false)
        }
    }.addOnFailureListener { e ->
        Log.e("MapPin", "Error uploading image", e)
        onComplete(false)
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

data class PinInfo(val description: String, val photoUrl: String)

fun distanceBetweenPoints(startLatLng: LatLng, endLatLng: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        startLatLng.latitude, startLatLng.longitude,
        endLatLng.latitude, endLatLng.longitude,
        results
    )
    return results[0]
}

fun fetchAndDisplayPins(googleMap: GoogleMap, currentLocation: LatLng) {
    googleMap.clear()
    val db = Firebase.firestore
    db.collection("pins")
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val mapPin = document.toObject(MapPin::class.java)
                val pinLocation = LatLng(mapPin.latitude, mapPin.longitude)

                // Check if the pin is within 200 meters (two blocks) of the current location
                if (distanceBetweenPoints(currentLocation, pinLocation) <= 200) {
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(pinLocation)
                            .title(mapPin.title)
                            .snippet(mapPin.description)
                    )
                    marker?.tag = PinInfo(
                        description = mapPin.description,
                        photoUrl = mapPin.photoUrl
                    )
                }
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
