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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage


@Composable
fun GoogleMapComposable(imageHandler: ImageHandler) {
    var showDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("") }
    var pinDescription by remember { mutableStateOf("") }
    var pincreatorUsername by remember { mutableStateOf("") }
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
    var selectedMapPin by remember { mutableStateOf<MapPin?>(null) }
    var googleMapInstance: GoogleMap? by remember { mutableStateOf(null) }
    var currentLatLngInstance: LatLng? by remember { mutableStateOf(null) }
    fun fetchCurrentUserUsername(onResult: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val username = documentSnapshot.getString("username") ?: "Anonymous"
                onResult(username)
            }
            .addOnFailureListener {
                Log.e("GoogleMapComposable", "Error fetching user data", it)
                onResult("Anonymous") // Fallback username
            }
    }

    fun fetchUpdatedMapPin(mapPinId: String, onComplete: (MapPin?) -> Unit) {
        val db = Firebase.firestore
        db.collection("pins").document(mapPinId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val updatedMapPin = documentSnapshot.toObject(MapPin::class.java)
                onComplete(updatedMapPin)
            }
            .addOnFailureListener { e ->
                Log.e("GoogleMapComposable", "Error fetching updated pin", e)
                onComplete(null) // Handle the failure case
            }
    }

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
                selectedMapPin = MapPin(
                    title = marker.title ?: "",
                    description = it.description,
                    creatorUsername = it.creatorUsername,
                    creatorUserId = it.creatorUserId,
                    latitude = marker.position.latitude,
                    longitude = marker.position.longitude,
                    photoUrl = it.photoUrl,
                    id = it.id
                )
                selectedMapPin?.id?.let { mapPinId ->
                    fetchUpdatedMapPin(mapPinId) { updatedMapPin ->
                        updatedMapPin?.let {
                            selectedMapPin = it
                            showPinInfoDialog = true
                        }
                    }
                }
            }
            true
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
        FirebaseAuth.getInstance().currentUser?.uid?.let { currentUserUid ->
            selectedMapPin?.let { pin ->
                PinInfoDialog(
                    mapPin = pin,
                    currentUser = currentUserUid,
                    onDismiss = {
                        showPinInfoDialog = false
                    },
                    onDelete = { pinToDelete ->
                        deletePin(pinToDelete) {
                            showPinInfoDialog = false
                            googleMapInstance?.let { map ->
                                currentLatLngInstance?.let { latLng ->
                                    fetchAndDisplayPins(map, latLng)
                                }
                            }
                        }
                    },
                    onLikeToggle = {toggleLikeOnPin(pin, currentUserUid) { updatedPin ->
                            selectedMapPin = updatedPin
                        }
                    }

                )
            }
        }
    }



    if (showDialog) {
        fetchCurrentUserUsername { username ->
            pincreatorUsername = username
        }
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
                            imageUri.let { uri ->
                                googleMapInstance?.let { googleMap ->
                                    currentLatLngInstance?.let { currentLatLng ->
                                        confirmAndCreatePin(mapPin, uri, googleMap, currentLatLng ) { success ->
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

// Function to confirm and create a pin
fun confirmAndCreatePin(mapPin: MapPin, imageUri: Uri?, googleMap: GoogleMap, currentLatLng: LatLng, onComplete: (Boolean) -> Unit) {
    val storageRef = Firebase.storage.reference
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val defaultPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/blockbuzznyc.appspot.com/o/pin_images%2Fnew_york_default.jpg?alt=media&token=a7d9a010-22fc-4c9d-8f98-5971c03e0427"

    if (imageUri == null || imageUri.toString().startsWith("gs://")) {
        // Use default photo URL if no image is selected or if the imageUri is a direct Firebase Storage reference
        val updatedMapPin = mapPin.copy(photoUrl = defaultPhotoUrl, creatorUserId = userId)
        savePinToFirestore(updatedMapPin, userId) { success, newPinId ->
            if (success) {
                updatedMapPin.id = newPinId
                fetchAndDisplayPins(googleMap, currentLatLng)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    } else {
        // Handle the case where a new image needs to be uploaded
        val imageRef = storageRef.child("pin_images/${imageUri.lastPathSegment}")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                val updatedMapPin = mapPin.copy(photoUrl = downloadUri.toString(), creatorUserId = userId)
                savePinToFirestore(updatedMapPin, userId) { success, newPinId ->
                    if (success) {
                        updatedMapPin.id = newPinId
                        fetchAndDisplayPins(googleMap, currentLatLng)
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
            }.addOnFailureListener { onComplete(false) }
        }.addOnFailureListener { onComplete(false) }
    }
}

fun savePinToFirestore(mapPin: MapPin, userId: String, onComplete: (Boolean, String) -> Unit) {
    val db = Firebase.firestore
    val userRef = db.collection("users").document(userId)
    val newPinRef = db.collection("pins").document()

    db.runTransaction { transaction ->
        val userSnapshot = transaction.get(userRef)
        val newPinCount = (userSnapshot.getLong("numberOfPins") ?: 0) + 1

        val updatedMapPin = mapPin.apply {
            creatorUserId = userId
            id = newPinRef.id
        }
        transaction.set(newPinRef, updatedMapPin)
        transaction.update(userRef, "numberOfPins", newPinCount)
    }.addOnSuccessListener {
        onComplete(true, newPinRef.id)
    }.addOnFailureListener {
        onComplete(false, "")
    }
}



data class PinInfo(
    val description: String,
    val creatorUsername: String,
    val creatorUserId: String, // Add this
    val photoUrl: String,
    val id: String
)


fun fetchAndDisplayPins(googleMap: GoogleMap, currentLocation: LatLng) {
    googleMap.clear()
    val db = Firebase.firestore
    db.collection("pins")
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val mapPin = document.toObject(MapPin::class.java).apply {
                    id = document.id // Ensure this line is correctly setting the document ID
                }
                val pinLocation = LatLng(mapPin.latitude, mapPin.longitude)

                // Check if the pin is within a certain distance of the current location
                if (distanceBetweenPoints(currentLocation, pinLocation) <= 200) {
                    // Add the marker to the map
                    val marker = googleMap.addMarker(
                        MarkerOptions().position(pinLocation).title(mapPin.title)
                    )
                    marker?.tag = PinInfo(
                        description = mapPin.description,
                        creatorUsername = mapPin.creatorUsername,
                        creatorUserId = mapPin.creatorUserId, // Set this value
                        photoUrl = mapPin.photoUrl,
                        id = mapPin.id
                    )

                    Log.d("MapPin", "Fetched pin with ID: ${mapPin.id}")

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