package com.example.blockbuzznyc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.blockbuzznyc.model.MapPin
import com.google.android.gms.maps.model.LatLng

@Composable
fun PinCard(pin: MapPin, onPinSelected: (LatLng) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .height(200.dp) // Fixed height for the card
            .width(150.dp) // Fixed width for the card
            .clickable {
                onPinSelected(LatLng(pin.latitude, pin.longitude)) // Use latitude and longitude from the pin
            } // Assuming `pin.location` is a `LatLng` object
    ) {
        Column {
            AsyncImage(
                model = pin.photoUrl,
                contentDescription = pin.title,
                modifier = Modifier
                    .height(120.dp) // Fixed height for the image
                    .fillMaxWidth(), // The image should fill the card width
                contentScale = ContentScale.Crop // Crop the image if necessary to fill the bounds
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = pin.title,
                    maxLines = 1, // Allow only one line for the title
                    overflow = TextOverflow.Ellipsis, // Add ellipsis if the title is too long
                    modifier = Modifier
                        .fillMaxWidth() // Ensure the title fills the width, so overflow can work
                        .padding(bottom = 4.dp) // Space below the title
                )
                // Description or other content can be added here
                // If the description is long, consider adding maxLines and overflow here as well
            }
        }
    }
}

