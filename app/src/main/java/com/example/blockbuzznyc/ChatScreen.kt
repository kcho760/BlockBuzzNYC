package com.example.blockbuzznyc

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.blockbuzznyc.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, pinId: String, pinTitle: String) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Listening for new messages
    LaunchedEffect(pinId) {
        listenForMessages(pinId) { newMessages ->
            if (newMessages.size > messages.size) {
                messages = newMessages
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = pinTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                items(messages) { message ->
                    Text(text = "${message.username}: ${message.message}", modifier = Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(
                            "Type a message",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSecondary, // Set label (text) color
                            )
                        )
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSecondary, // Set text color
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.tertiary, // Set text cursor color
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary, // Set focused outline color
                        unfocusedBorderColor = MaterialTheme.colorScheme.tertiary, // Set unfocused outline color
                        focusedLabelColor = MaterialTheme.colorScheme.onSecondary, // Set label (text) color
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        sendMessage(pinId, messageText)
                        SoundPlayer.playSendMessageSound(context) // Play send sound
                        messageText = ""
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Send")
                }
            }
        }
    }

}


fun listenForMessages(pinId: String, onMessageReceived: (List<ChatMessage>) -> Unit) {
    val ref = Firebase.database.reference.child("chats/$pinId")
    val messageListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val messages = dataSnapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
            onMessageReceived(messages)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.w("ChatScreen", "loadMessages:onCancelled", databaseError.toException())
        }
    }
    ref.addValueEventListener(messageListener)
}

fun sendMessage(pinId: String, messageText: String) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    // Fetch the username from Firestore
    val userRef = Firebase.firestore.collection("users").document(currentUserId)
    userRef.get().addOnSuccessListener { document ->
        val username = document.getString("username") ?: "Anonymous"
        val chatMessage = ChatMessage(
            senderId = currentUserId,
            username = username, // Assuming you have added 'username' to your ChatMessage data class
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        // Send the message to Firebase Realtime Database
        val ref = Firebase.database.reference.child("chats/$pinId")
        ref.push().setValue(chatMessage)
    }.addOnFailureListener {
        Log.e("ChatScreen", "Failed to fetch username", it)
    }
}
