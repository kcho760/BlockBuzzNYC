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
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockBuzzNYCTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
//                    Greeting("Android")
                    GoogleMapComposable()
                }
            }
        }
    }
}

@Composable
fun GoogleMapComposable() {
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
        } else {
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
                    println("Map is ready")

                    // Configure the map's camera position and zoom level
                    val initialLatLng = LatLng(40.7128, -74.0060) // New York City coordinates
                    val zoomLevel = 12f
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, zoomLevel))

                    // Add a marker to the map
                    val markerOptions = MarkerOptions()
                        .position(initialLatLng)
                        .title("New York City")
                    googleMap.addMarker(markerOptions)
                }


                mapView = it
            }
        },
        update = { mapView ->
            mapView?.onResume()
        }
    )

//    DisposableEffect(key1 = mapView) {
//        onDispose {
//            mapView?.onDestroy()
//        }
//    }
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello, $name!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlockBuzzNYCTheme {
//        Greeting("Android")
    }
}