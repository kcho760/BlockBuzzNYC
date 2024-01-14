package com.example.blockbuzznyc

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoContents(marker: Marker): View {
        Log.d("CustomInfoWindowAdapter", "getInfoContents called for marker: ${marker.title}")

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.custom_info_window, null)

        val pinInfo = marker.tag as? PinInfo
//        val usernameTextView = view.findViewById<TextView>(R.id.creatorUsernameTextView)

//        usernameTextView.text = pinInfo?.creatorUsername ?: "Username not found"

        return view
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }
}

