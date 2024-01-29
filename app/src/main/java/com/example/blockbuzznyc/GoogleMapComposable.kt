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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.blockbuzznyc.model.Achievement
import com.example.blockbuzznyc.model.MapPin
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage


@Composable
fun GoogleMapComposable(
    imageHandler: ImageHandler,
    navController: NavController,
    showPinInfoDialog: MutableState<Boolean>,
    selectedMapPin: MutableState<MapPin?>,
) {
    var showDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("") }
    var pinDescription by remember { mutableStateOf("") }
    var pincreatorUsername by remember { mutableStateOf("") }
    var selectedLatLng: LatLng? by remember { mutableStateOf(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var mapViewInstance: MapView? by remember { mutableStateOf(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasPermissions by remember { mutableStateOf(false) }
    val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var googleMapInstance: GoogleMap? by remember { mutableStateOf(null) }
    var currentLatLngInstance: LatLng? by remember { mutableStateOf(null) }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    val availableTags = listOf("Food", "Art", "Other", "Nature", "Entertainment") //tag list
    var isInitialSetupDone by remember { mutableStateOf(false) }
    val selectedPinLocation = selectedMapPin.value?.let { LatLng(it.latitude, it.longitude) }
    var titleErrorMessage by remember { mutableStateOf<String?>(null) }
    var descriptionErrorMessage by remember { mutableStateOf<String?>(null) }
    var tagErrorMessage by remember { mutableStateOf<String?>(null) }
    var hasCheckedPermissions by rememberSaveable { mutableStateOf(false) }
    var newAchievement by remember { mutableStateOf(null) }

    fun fetchCurrentUserUsername(onResult: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val username = documentSnapshot.getString("username") ?: "Anonymous"
                onResult(username)
            }
            .addOnFailureListener {
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
            .addOnFailureListener {
                onComplete(null) // Handle the failure case
            }
    }

    LaunchedEffect(Unit) {
        Log.d("PinTracking", "Started with pin: ${selectedMapPin.value?.id}")
        Log.d("PinTracking", "Showdialog status: $showPinInfoDialog")
    }

    LaunchedEffect(isMapReady, selectedPinLocation) {
        if (isMapReady && selectedPinLocation != null) {
            googleMapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPinLocation, 18f))
        } else {
            Log.d("GoogleMapComposable", "Map not ready or location is null: isMapReady = $isMapReady, selectedPinLocation = $selectedPinLocation")
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

    if (!hasCheckedPermissions) {
        LaunchedEffect(Unit) {
            hasPermissions = permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (!hasPermissions) {
                requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
            }
            hasCheckedPermissions = true
        }
    }

    fun setupGoogleMap(googleMap: GoogleMap) {
        googleMap.setOnMarkerClickListener { marker ->

            val pinInfo = marker.tag as? PinInfo
            pinInfo?.let { it ->
                selectedMapPin.value = MapPin(
                    title = marker.title ?: "",
                    description = it.description,
                    creatorUsername = it.creatorUsername,
                    creatorUserId = it.creatorUserId,
                    latitude = marker.position.latitude,
                    longitude = marker.position.longitude,
                    photoUrl = it.photoUrl,
                    id = it.id,
                    tags = it.tags
                )
                selectedMapPin.value?.id?.let { mapPinId ->
                    fetchUpdatedMapPin(mapPinId) { updatedMapPin ->
                        updatedMapPin?.let {
                            selectedMapPin.value = it
                        }
                    }
                }
                showPinInfoDialog.value = true
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
                            val success = googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    context, R.raw.map_style_dark_mode
                                )
                            )
                            if (!success) {
                                Log.e("GoogleMapComposable", "Style parsing failed.")
                            }
                            setupGoogleMap(googleMap)
                            isMapReady = true

                            if (!isInitialSetupDone && selectedPinLocation == null) {
                                // Initial setup: setting camera position and fetching pins
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    val currentLatLng = location?.let {
                                        LatLng(it.latitude, it.longitude)
                                    } ?: LatLng(40.7128, -74.0060) // Default to New York City if location is null
                                    currentLatLngInstance = currentLatLng
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                                    fetchAndDisplayPins(googleMap, currentLatLng, context)
                                    isInitialSetupDone = true
                                }
                            } else if (selectedPinLocation != null) {
                                // Fetching and displaying pins when a new pin location is selected
                                fetchAndDisplayPins(googleMap, selectedPinLocation, context)
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

        if (showPinInfoDialog.value && selectedMapPin.value != null) {
            Log.d("GoogleMapComposable", "Showing info dialog for pin: ${selectedMapPin.value?.id}")
            FirebaseAuth.getInstance().currentUser?.uid?.let { currentUserUid ->
            val pin = selectedMapPin.value!!
            PinInfoDialog(
                mapPin = pin,
                currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                onDismiss = { showPinInfoDialog.value = false },
                onDelete = { pinToDelete ->
                    deletePin(pinToDelete, currentUserUid) {
                        showPinInfoDialog.value = false
                        googleMapInstance?.let { map ->
                            currentLatLngInstance?.let { latLng ->
                                fetchAndDisplayPins(map, latLng, context)
                            }
                        }
                    }
                },
                onLikeToggle = {toggleLikeOnPin(pin, currentUserUid) {updatedPin ->
                    selectedMapPin.value = updatedPin
                }
                },
                onChatButtonClick = { pin ->
                    navController.navigate("chatScreen/${pin.id}/${pin.title}")
                }
            )
        }
    }



    if (showDialog) {
        fetchCurrentUserUsername { username ->
            pincreatorUsername = username
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Create a Pin",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            text = {
                Column {
                    TextField(
                        value = pinTitle,
                        onValueChange = { pinTitle = it },
                        label = {
                            Text(
                                text = "Title",
                                color = MaterialTheme.colorScheme.onPrimary
                            ) },
                        singleLine = true,
                        isError = titleErrorMessage != null,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onPrimary)
                    )
                    titleErrorMessage?.let { errorMessage ->
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, // Use colorScheme.error
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }

                    TextField(
                        value = pinDescription,
                        onValueChange = { pinDescription = it },
                        label = {
                            Text(
                                text = "Description",
                                color = MaterialTheme.colorScheme.onPrimary
                            ) },
                        singleLine = true,
                        isError = descriptionErrorMessage != null,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onPrimary)
                    )
                    descriptionErrorMessage?.let { errorMessage ->
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, // Use colorScheme.error
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
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
                    Text(
                        "Select Tags (up to 3):",
                        color = Color.Black // Set the text color to black
                    )
                    availableTags.forEach { tag ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedTags.contains(tag),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedTags.size < 3) {
                                            selectedTags = selectedTags + tag
                                        }
                                    } else {
                                        selectedTags = selectedTags - tag
                                    }
                                }
                            )
                            Text(
                                text = tag,
                                color = Color.Black // Set the text color to black
                            )
                        }
                    }
                    tagErrorMessage?.let { errorMessage ->
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, // Use colorScheme.error
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Initialize validation flag
                        var isValid = true

                        // Reset error messages
                        titleErrorMessage = null
                        descriptionErrorMessage = null

                        // Validate title
                        if (pinTitle.isBlank()) {
                            titleErrorMessage = "Title cannot be empty"
                            isValid = false
                        }

                        // Validate description
                        if (pinDescription.isBlank()) {
                            descriptionErrorMessage = "Description cannot be empty"
                            isValid = false
                        }

                        // Validate tags
                        if (selectedTags.isEmpty()) {
                            tagErrorMessage = "Please select at least one tag"
                            isValid = false
                        }

                        // Proceed only if validation passes
                        if (isValid) {
                            selectedLatLng?.let { latLng ->
                                val mapPin = MapPin(
                                    title = pinTitle,
                                    description = pinDescription,
                                    latitude = latLng.latitude,
                                    longitude = latLng.longitude,
                                    photoUrl = "",
                                    creatorUsername = pincreatorUsername,
                                    tags = selectedTags
                                )
                                imageUri.let { uri ->
                                    googleMapInstance?.let { googleMap ->
                                        currentLatLngInstance?.let { currentLatLng ->
                                            confirmAndCreatePin(mapPin, uri, googleMap, currentLatLng, context) { success ->
                                                if (success) {
                                                    Log.d("MapPin", "Pin created successfully")
                                                    fetchAndDisplayPins(googleMap, currentLatLng, context)

                                                    // Reset form fields after successful creation
                                                    pinTitle = ""
                                                    pinDescription = ""
                                                    imageUri = null
                                                    selectedTags = emptyList()
                                                } else {
                                                    Log.d("MapPin", "Pin creation failed")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            showDialog = false
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
fun confirmAndCreatePin(mapPin: MapPin, imageUri: Uri?, googleMap: GoogleMap, currentLatLng: LatLng, context: Context, onComplete: (Boolean) -> Unit) {
    val storageRef = Firebase.storage.reference
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val defaultPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/blockbuzznyc.appspot.com/o/pin_images%2Fnew_york_default.jpg?alt=media&token=969960c8-7df8-4a07-8e6c-e41a419521aa"

    if (imageUri == null || imageUri.toString().startsWith("gs://")) {
        // Use default photo URL if no image is selected or if the imageUri is a direct Firebase Storage reference
        val updatedMapPin = mapPin.copy(photoUrl = defaultPhotoUrl, creatorUserId = userId)
        savePinToFirestore(updatedMapPin, userId, context) { success, newPinId ->
            if (success) {
                updatedMapPin.id = newPinId
                fetchAndDisplayPins(googleMap, currentLatLng, context)
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
                savePinToFirestore(updatedMapPin, userId, context) { success, newPinId ->
                    if (success) {
                        updatedMapPin.id = newPinId
                        fetchAndDisplayPins(googleMap, currentLatLng, context)
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
            }.addOnFailureListener { onComplete(false) }
        }.addOnFailureListener { onComplete(false) }
    }
}

fun savePinToFirestore(mapPin: MapPin, userId: String, context: Context, onComplete: (Boolean, String) -> Unit) {
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
        // Update the last Ten pins collection
        updateLastTenPinsCollection(newPinRef.id)
        // Now that the pin has been saved, check for achievements.
        fetchUserAndCheckAchievements(userId, context)
    }.addOnFailureListener { e ->
        Log.e("SavePin", "Failed to save pin: ${e.message}", e)
        onComplete(false, "")
    }
}

fun fetchUserAndCheckAchievements(userId: String, context: Context) {
    val userRef = Firebase.firestore.collection("users").document(userId)
    userRef.get().addOnSuccessListener { documentSnapshot ->
        val user = documentSnapshot.toObject(User::class.java)
        user?.let {
            // Now passing 'context' to 'checkForAchievements'
            checkForAchievements(user, context)
        }
    }.addOnFailureListener { e ->
        Log.e("Achievements", "Failed to fetch user for achievements: ${e.message}", e)
    }
}


data class PinInfo(
    val description: String,
    val creatorUsername: String,
    val creatorUserId: String, // Add this
    val photoUrl: String,
    val id: String,
    val tags: List<String>
)


fun fetchAndDisplayPins(googleMap: GoogleMap, currentLocation: LatLng, context: Context) {
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
                //Distance check disabled for now
                // if (distanceBetweenPoints(currentLocation, pinLocation) <= 200) {
                // Food pin disabled for now
                // addCustomMarker(googleMap, mapPin, context, R.drawable.food_pin)
                val marker = googleMap.addMarker(
                    MarkerOptions().position(pinLocation).title(mapPin.title)
                )
                marker?.tag = PinInfo(
                    description = mapPin.description,
                    creatorUsername = mapPin.creatorUsername,
                    creatorUserId = mapPin.creatorUserId, // Set this value
                    photoUrl = mapPin.photoUrl,
                    id = mapPin.id,
                    tags = mapPin.tags
                )

                // }
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

//fun addCustomMarker(googleMap: GoogleMap, mapPin: MapPin, context: Context, iconResId: Int) {
//    val customIcon = BitmapDescriptorFactory.fromResource(iconResId) // Use the resource ID of the custom icon
//
//    val markerOptions = MarkerOptions()
//        .position(LatLng(mapPin.latitude, mapPin.longitude))
//        .icon(customIcon) // Set the custom icon here
//    googleMap.addMarker(markerOptions)
//}

fun updateLastTenPinsCollection(newPinId: String) {
    val db = Firebase.firestore
    val lastTenPinsRef = db.collection("lastTenPins")

    // Fetch the current last Ten pins
    lastTenPinsRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(5)
        .get()
        .addOnSuccessListener { documents ->
            val ids = documents.map { it.id }

            if (ids.size == 10) {
                // Remove the oldest pin ID if there are already 5
                lastTenPinsRef.document(ids.last()).delete()
            }

            // Add the new pin ID
            lastTenPinsRef.document(newPinId).set(mapOf("createdAt" to Timestamp.now()))
        }
}

fun checkForAchievements(user: User, context: Context) {
    val achievements = user.achievements.toMutableList()
    var newAchievement: Achievement? = null

    // Check each criterion and add/update achievements as necessary
    if (user.numberOfPins >= 1 && achievements.none { it.id == "rookiePoster" }) {
        newAchievement = Achievement("rookiePoster", "Rookie Poster", "Create your first pin.", true, Timestamp.now())
        achievements.add(newAchievement)
    }

//    if (user.numberOfPins >= 5 && achievements.none { it.id == "proPoster" }) {
//        achievements.add(Achievement("proPoster", "Pro Poster", "Create 5 pins.", true, Timestamp.now()))
//    }
//
//    if (user.numberOfPins >= 10 && achievements.none { it.id == "masterPoster" }) {
//        achievements.add(Achievement("masterPoster", "Master Poster", "Create 10 pins.", true, Timestamp.now()))
//    }
//
//    if (user.totalLikes >= 10 && achievements.none { it.id == "influencer" }) {
//        achievements.add(Achievement("influencer", "Influencer", "Receive 100 likes on your pins.", true, Timestamp.now()))
//    }
//
//    if(user.profilePictureUrl != "" && achievements.none { it.id == "profilePic" }) {
//        achievements.add(Achievement("profilePic", "Say Cheese", "Upload a profile picture.", true, Timestamp.now()))
//    }

    // Update the user document with the new achievements
    updateUserAchievements(user.userId, achievements)
}

fun updateUserAchievements(userId: String, achievements: List<Achievement>) {
    val userRef = Firebase.firestore.collection("users").document(userId)
    userRef.update("achievements", achievements)
        .addOnSuccessListener { Log.d("Achievements", "User achievements updated.") }
        .addOnFailureListener { e -> Log.e("Achievements", "Error updating achievements.", e) }
}