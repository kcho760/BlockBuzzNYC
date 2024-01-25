// SearchScreen.kt
package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.blockbuzznyc.model.MapPin
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


@Composable
fun SearchScreen(onPinSelected: (MapPin) -> Unit) {
    val availableTags = listOf("Food", "Art", "Other", "Nature", "Entertainment") // Example tag list
    var selectedTags by remember { mutableStateOf(listOf<String>()) }
    var searchResults by remember { mutableStateOf<List<MapPin>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var recentPins by remember { mutableStateOf<List<MapPin>>(emptyList()) }

    LaunchedEffect(selectedTags) {
        if (selectedTags.isEmpty()) {
            fetchRecentPins { results ->
                recentPins = results
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        // Replace FlowRow with LazyRow for horizontal scrolling
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableTags) { tag ->
                TagButton(tag, selectedTags.contains(tag)) { isSelected ->
                    selectedTags = if (isSelected) {
                        selectedTags + tag
                    } else {
                        selectedTags - tag
                    }
                    // Trigger the search with the newly selected tags
                    coroutineScope.launch {
                        performTagSearch(selectedTags) { results ->
                            searchResults = results
                        }
                    }
                }
            }
        }

        if (selectedTags.isEmpty() && recentPins.isNotEmpty()) {
            Text("Recent Pins")
            LazyColumn() {
                items(recentPins) { pin ->
                    PinItem(pin, onClick = {
                        onPinSelected(pin)
                    })
                }
            }
        }

        LazyColumn {
            items(searchResults) { pin ->
                PinItem(pin, onClick = {
                    onPinSelected(pin)
                })
            }
        }
    }
}
fun performTagSearch(selectedTags: List<String>, onSearchResults: (List<MapPin>) -> Unit) {
    if (selectedTags.isEmpty()) {
        onSearchResults(emptyList())
        return
    }

    val db = Firebase.firestore
    val searchTasks = selectedTags.map { tag ->
        db.collection("pins")
            .whereArrayContains("tags", tag)
            .get()
    }

    val allResults = mutableListOf<MapPin>()

    Tasks.whenAllSuccess<QuerySnapshot>(searchTasks)
        .addOnSuccessListener { listOfQuerySnapshot ->
            listOfQuerySnapshot.forEach { querySnapshot ->
                val pins = querySnapshot.toObjects(MapPin::class.java)
                allResults.addAll(pins)
            }
            // Distinct by pin ID to remove duplicates if a pin has multiple selected tags
            onSearchResults(allResults.distinctBy { it.id })
        }
        .addOnFailureListener { exception ->
            Log.w("SearchScreen", "Error getting search results: ", exception)
            onSearchResults(emptyList())
        }
}

@Composable
fun TagButton(tag: String, isSelected: Boolean, onSelectionChanged: (Boolean) -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50)),
        color = backgroundColor
    ) {
        Text(
            text = tag,
            modifier = Modifier
                .clickable { onSelectionChanged(!isSelected) }
                .padding(8.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinItem(pin: MapPin, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp) // Give some space between the text and the image
            ) {
                Text(text = pin.title)
                Text(text = pin.description)
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pin.tags.forEach { tag ->
                        ChipView(tag)
                    }
                }
            }
            AsyncImage(
                model = pin.photoUrl,
                contentDescription = "Pin Image",
                modifier = Modifier
                    .size(100.dp) // Set the size of the image
                    .clip(RoundedCornerShape(8.dp)), // Optional: if you want rounded corners for the image
                contentScale = ContentScale.Crop // Crop the image if not fully inside the bounds
            )
        }
    }
}

@Composable
fun ChipView(tag: String) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)),
        color = MaterialTheme.colorScheme.secondary
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(8.dp),
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

fun fetchRecentPins(onComplete: (List<MapPin>) -> Unit) {
    val db = Firebase.firestore
    db.collection("lastFivePins")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val pinIds = documents.map { it.id }
            fetchPinsByIds(pinIds, onComplete)
        }
        .addOnFailureListener { exception ->
            Log.w("SearchScreen", "Error getting recent pins: ", exception)
            onComplete(emptyList())
        }
}

fun fetchPinsByIds(pinIds: List<String>, onComplete: (List<MapPin>) -> Unit) {
    val db = Firebase.firestore
    val pinsRef = db.collection("pins")

    // Create a list to hold all the async operations
    val tasks = pinIds.map { pinId ->
        pinsRef.document(pinId).get()
    }

    // Wait for all the async operations to complete
    Tasks.whenAllSuccess<DocumentSnapshot>(tasks).addOnSuccessListener { documentSnapshots ->
        val pins = documentSnapshots.mapNotNull { snapshot ->
            snapshot.toObject<MapPin>()?.apply {
                // Ensure the 'id' field is set correctly
                this.id = snapshot.id
            }
        }
        onComplete(pins)
    }.addOnFailureListener { exception ->
        Log.w("SearchScreen", "Error fetching pins by IDs: ", exception)
        onComplete(emptyList())
    }
}