package com.example.blockbuzznyc

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.custom_info_window, null)

        val image = view.findViewById<ImageView>(R.id.infoWindowImage)
        val title = view.findViewById<TextView>(R.id.infoWindowTitle)
        val description = view.findViewById<TextView>(R.id.infoWindowDescription)

        title.text = marker.title

        // Extract PinInfo from the marker's tag
        val pinInfo = marker.tag as? PinInfo
        if (pinInfo != null) {
            description.text = pinInfo.description
            image.load(pinInfo.photoUrl) {
                crossfade(true)
                error(R.drawable.placeholder_image) // Placeholder for error
                listener(
                    onSuccess = { _, _ ->
                        // Image successfully loaded; update the info window.
                        (context as? Activity)?.runOnUiThread {
                            marker.showInfoWindow()
                        }
                    },
                    onError = { _, result ->
                        // Image failed to load; log the error.
                        val throwable = result.throwable // Here you get the Throwable from the ErrorResult
                        Log.e("MapPin", "Error loading image: ", throwable)
                    }
                )
            }



        }

        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        // Return null here if you want to use the default frame and background
        return null
    }
}
