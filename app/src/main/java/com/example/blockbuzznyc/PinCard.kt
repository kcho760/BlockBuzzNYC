package com.example.blockbuzznyc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.blockbuzznyc.model.MapPin

@Composable
fun PinCard(pin: MapPin) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth() // Fill the maximum width available
    ) {
        Column {
            AsyncImage(
                model = pin.photoUrl,
                contentDescription = pin.title,
                modifier = Modifier
                    .height(90.dp) // Adjust the height as needed
                    .fillMaxWidth(), // Fill the width of the card
                contentScale = ContentScale.Crop // Crop the image if necessary to fill the bounds
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = pin.title,
                    modifier = Modifier.padding(bottom = 4.dp) // Add some space below the title
                )
                Text(
                    text = pin.description,
                    modifier = Modifier.padding(bottom = 8.dp) // Add some space below the description
                )
            }
        }
    }
}
