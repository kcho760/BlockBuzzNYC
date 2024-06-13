package com.example.blockbuzznyc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.blockbuzznyc.model.MapPin

@Composable
fun PinCard(pin: MapPin, onPinSelected: (MapPin) -> Unit) {
    Card(
            modifier = Modifier
                    .width(150.dp) // Adjust the width as needed
                    .padding(8.dp)
                    .clickable { onPinSelected(pin) },
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(4.dp)
    ) {
        BoxWithConstraints(
                modifier = Modifier
                        .height(150.dp) // Adjust the total height as needed
        ) {
            val maxHeight = maxHeight

            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                        model = pin.photoUrl,
                        contentDescription = pin.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(maxHeight * 0.75f) // Allocate 75% of the available height to the image
                )

                Box(
                        modifier = Modifier
                                .fillMaxWidth()
                                .height(maxHeight * 0.25f) // Allocate 25% of the available height to the text
                ) {
                    Text(
                            text = pin.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
