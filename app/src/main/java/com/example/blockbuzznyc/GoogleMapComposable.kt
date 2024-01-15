package com.example.blockbuzznyc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.blockbuzznyc.model.MapPin
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage


@Composable
fun GoogleMapComposable(imageHandler: ImageHandler) {
    var showDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("") }
    var pinDescription by remember { mutableStateOf("") }
    val pincreatorUsername by remember { mutableStateOf("") }
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

    fun setupGoogleMap(googleMap: GoogleMap, context: Context) {
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter(context))
        // Set custom marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            val pinInfo = marker.tag as? PinInfo
            pinInfo?.let {
                selectedMapPin = marker.title?.let { it1 ->
                    MapPin(
                        title = it1,
                        description = it.description,
                        creatorUsername = it.creatorUsername,
                        latitude = marker.position.latitude,
                        longitude = marker.position.longitude,
                        photoUrl = it.photoUrl,
                    )
                }
                Log.d(selectedMapPin.toString(), "selectedMapPin username $selectedMapPin")
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

    Box {
        Column{
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp) // Adjust the padding as needed
        ) {
            // The Zoom In Button
            FloatingActionButton(
                onClick = { zoomInMap(googleMapInstance) },
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom In")
            }

            // The Recenter Button
            FloatingActionButton(
                onClick = { mapViewInstance?.getMapAsync { googleMap -> recenterMap(googleMap) }},
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Recenter")
            }

            // The Zoom Out Button
            FloatingActionButton(
                onClick = { zoomOutMap(googleMapInstance) },
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom Out")
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
                                photoUrl = "",
                                creatorUsername = pincreatorUsername // Correct this line
                            )
                            imageUri?.let { uri ->
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

fun zoomInMap(googleMap: GoogleMap?) {
    googleMap?.let {
        val currentZoom = it.cameraPosition.zoom
        it.animateCamera(CameraUpdateFactory.zoomTo(currentZoom + 1))
    }
}

fun zoomOutMap(googleMap: GoogleMap?) {
    googleMap?.let {
        val currentZoom = it.cameraPosition.zoom
        it.animateCamera(CameraUpdateFactory.zoomTo(currentZoom - 1))
    }
}

fun confirmAndCreatePin(mapPin: MapPin, imageUri: Uri, googleMap: GoogleMap, currentLatLng: LatLng, onComplete: (Boolean) -> Unit) {
    val storageRef = Firebase.storage.reference
    val imageRef = storageRef.child("pin_images/${imageUri.lastPathSegment}")
    val uploadTask = imageRef.putFile(imageUri)
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
    uploadTask.addOnSuccessListener { taskSnapshot ->
        Log.d("MapPin", "confirmAndCreatePin called with URI: $imageUri")
        taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
            fetchUsername(userId) { username ->
                val newMapPin = mapPin.copy(
                    photoUrl = downloadUri.toString(),
                    creatorUsername = username // Set the username here
                )
            savePinToFirestore(newMapPin).addOnSuccessListener {
                fetchAndDisplayPins(googleMap, currentLatLng)
                Log.d("MapPin", "Pin saved successfully")
                onComplete(true)
            }.addOnFailureListener { e ->
                Log.e("MapPin", "Error saving pin to Firestore", e)
                onComplete(false)
            }
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

data class PinInfo(
    val description: String,
    val creatorUsername: String,
    val photoUrl: String
)

fun fetchAndDisplayPins(googleMap: GoogleMap, currentLocation: LatLng) {
    googleMap.clear()
    val db = Firebase.firestore
    db.collection("pins")
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val mapPin = document.toObject(MapPin::class.java)
                val pinLocation = LatLng(mapPin.latitude, mapPin.longitude)

                // Check if the pin is within a certain distance of the current location
                if (distanceBetweenPoints(currentLocation, pinLocation) <= 200) {
                    // Add the marker to the map
                    val marker = googleMap.addMarker(
                        MarkerOptions().position(LatLng(mapPin.latitude, mapPin.longitude)).title(mapPin.title)
                    )

                    // Set the custom object as a tag for the marker
                    marker?.tag = PinInfo(
                        description = mapPin.description,
                        creatorUsername = mapPin.creatorUsername,
                        photoUrl = mapPin.photoUrl
                    )
                    marker?.showInfoWindow()
                }
            }
        }
        .addOnFailureListener { exception ->
            Log.w("Firestore", "Error getting documents: ", exception)
        }
}


fun distanceBetweenPoints(startLatLng: LatLng, endLatLng: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        startLatLng.latitude, startLatLng.longitude,
        endLatLng.latitude, endLatLng.longitude,
        results
    )
    return results[0]
}