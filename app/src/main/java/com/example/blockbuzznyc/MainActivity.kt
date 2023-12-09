package com.example.blockbuzznyc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextField

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
    val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val hasFineLocationPermission = ContextCompat.checkSelfPermission(
        LocalContext.current,
        fineLocationPermission
    ) == PackageManager.PERMISSION_GRANTED

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    if (!hasFineLocationPermission) {
        requestPermissionLauncher.launch(fineLocationPermission)
    }
    AndroidView(
        factory = { context ->
            MapView(context).also {
                it.onCreate(null)
                it.getMapAsync { googleMap ->
                    val initialLatLng = LatLng(40.7128, -74.0060) // New York City coordinates
                    val zoomLevel = 17f
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, zoomLevel))

                    googleMap.setOnMapLongClickListener { latLng ->
                        selectedLatLng = latLng
                        showDialog = true
                    }
                }

                mapView = it
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
                        showDialog = false
                        mapView?.getMapAsync { googleMap ->
                            selectedLatLng?.let { latLng ->
                                val markerOptions = MarkerOptions()
                                    .position(latLng)
                                    .title(pinTitle)
                                googleMap.addMarker(markerOptions)
                            }
                        }
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlockBuzzNYCTheme {
        // Greeting("Android")
    }
}
