package com.example.blockbuzznyc

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.blockbuzznyc.model.MapPin

@Composable
fun PinInfoDialog(mapPin: MapPin?, onDismiss: () -> Unit) {
    if (mapPin != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = mapPin.title,
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color.Black)
                )
            },

            text = {
                Column {
                    Text(
                        text = "By ${mapPin.creatorUsername.toString()}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    Text(
                        text = mapPin.description,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    mapPin.photoUrl?.let { url ->
                        Image(
                            painter = rememberAsyncImagePainter(model = url),
                            contentDescription = "Pin Image",
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}
