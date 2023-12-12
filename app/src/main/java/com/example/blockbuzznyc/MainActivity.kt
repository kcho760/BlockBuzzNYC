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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockBuzzNYCTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    GoogleMapComposable()
                }
            }
        }
    }
}

@Composable
fun GoogleMapComposable() {
    var showDialog by remember { mutableStateOf(false) }
    var pinTitle by remember { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

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
    AndroidView(
        factory = { context ->
            MapView(context).also { mapView ->
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

                    googleMap.setOnMapLongClickListener { latLng ->
                        selectedLatLng = latLng
                        showDialog = true
                    }
                }
            }
        },
        update = { mapView ->
            mapView?.onResume()
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(text = "Add a title")
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
                        Log.d("MapView", "Add Button Clicked") // Logging button click
                        showDialog = false
                        mapView?.getMapAsync { googleMap ->
                            selectedLatLng?.let { latLng ->
                                Log.d("MapView", "Selected LatLng: $latLng") // Logging the selected coordinates
                                val markerOptions = MarkerOptions()
                                    .position(latLng)
                                    .title(pinTitle)
                                googleMap.addMarker(markerOptions)
                                Log.d("MapView", "Marker added with title: $pinTitle") // Logging marker addition
                            } ?: Log.d("MapView", "Selected LatLng is null") // Logging if LatLng is null
                        } ?: Log.d("MapView", "MapView is null") // Logging if MapView is null
                    }
                ) {
                    Text(text = "Add")
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


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlockBuzzNYCTheme {
        // Greeting("Android")
    }
}
