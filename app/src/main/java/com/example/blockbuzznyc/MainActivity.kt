package com.example.blockbuzznyc

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
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


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
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    AndroidView(
        factory = { context ->
            MapView(context).apply {
                onCreate(null)
                getMapAsync { map ->
                    googleMap = map
                    // Customize your map here
                    val sydney = LatLng(-34.0, 151.0)
                    googleMap?.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(sydney))
                }
                mapView = this
            }
        },
        update = { mapView ->
            mapView?.onResume()
        }
    )

    DisposableEffect(key1 = mapView) {
        onDispose {
            mapView?.onDestroy()
        }
    }
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