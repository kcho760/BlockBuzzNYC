// SearchScreen.kt
package com.example.blockbuzznyc

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blockbuzznyc.model.MapPin
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MapPin>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                searchJob?.cancel()
                searchJob = coroutineScope.launch {
                    delay(500)  // Debounce delay
                    performTagSearch(searchText) { results ->
                        searchResults = results
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search tags...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
        )

        LazyColumn {
            items(searchResults) { pin ->
                PinItem(pin)
            }
        }
    }
}
fun performTagSearch(tag: String, onSearchResults: (List<MapPin>) -> Unit) {
    val db = Firebase.firestore
    db.collection("pins")
        .whereArrayContains("tags", tag)
        .get()
        .addOnSuccessListener { documents ->
            val results = documents.mapNotNull { it.toObject(MapPin::class.java) }
            onSearchResults(results)
        }
        .addOnFailureListener { exception ->
            Log.w("SearchScreen", "Error getting search results: ", exception)
            onSearchResults(emptyList()) // Pass an empty list if the search fails
        }
}

@Composable
fun PinResultsScreen(pins: List<MapPin>) {
    LazyColumn {
        items(pins) { pin ->
            PinItem(pin)
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinItem(pin: MapPin) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
        ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
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

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen()
}