package com.example.blockbuzznyc

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import kotlinx.coroutines.delay
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
    var hasUnseenMessages by remember { mutableStateOf(false) }
    var lastMessageCountAtBottom by remember { mutableStateOf(0) }
    var isInitialLoad by remember { mutableStateOf(true) }

    LaunchedEffect(pinId) {
        listenForMessages(pinId, context) { newMessages ->
            val isAtBottom = listState.layoutInfo.visibleItemsInfo.any {
                it.index == messages.size - 1
            }

            val hadMessages = messages.isNotEmpty()
            val hasNewMessages = newMessages.size > messages.size

            // Update the message list
            messages = newMessages

            if (isAtBottom) {
                // User is at the bottom, update the last message count
                lastMessageCountAtBottom = messages.size
                hasUnseenMessages = false

                // If new messages have arrived, auto-scroll to the bottom
                if (hasNewMessages && hadMessages) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(newMessages.size - 1)
                    }
                }
            } else {
                // User is not at the bottom, check for unseen messages
                if (newMessages.size > lastMessageCountAtBottom && !isInitialLoad) {
                    hasUnseenMessages = true
                }
            }

            // Handle initial load to scroll to the bottom
            if (isInitialLoad && newMessages.isNotEmpty()) {
                coroutineScope.launch {
                    delay(100)
                    listState.scrollToItem(index = newMessages.size - 1)
                }
                isInitialLoad = false
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(675.dp),
                state = listState,
                contentPadding = PaddingValues(top = 120.dp) // Adjust this value to match the height of the input field
            ) {
                items(messages) { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Text(
                            text = "${message.username}: ${message.message}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .background(
                                    color = if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                        )
                    }
                }

            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type a message") },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSecondary),
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.tertiary,
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        focusedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    )
                )
                // "Scroll to bottom" button floating on top of the LazyColumn content
                if (listState.firstVisibleItemIndex < messages.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        modifier = Modifier
//                            .padding(bottom = 80.dp, end = 16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Scroll to bottom")
                    }

                }
                Button(
                    onClick = {
                        sendMessage(pinId, messageText, context)
                        SoundPlayer.playSendMessageSound(context)
                        messageText = ""
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Send")
                }
            }
            if (hasUnseenMessages && !isInitialLoad) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                                hasUnseenMessages = false
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("New Messages", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }

        }
    }
}

fun listenForMessages(pinId: String, context: Context, onMessageReceived: (List<ChatMessage>) -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var lastReceivedMessage: ChatMessage? = null
    val ref = Firebase.database.reference.child("chats/$pinId")

    val messageListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val newMessages = dataSnapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }

            val latestMessage = newMessages.lastOrNull()
            if (latestMessage != null && latestMessage.senderId != currentUserId && latestMessage != lastReceivedMessage) {
                SoundPlayer.playReceiveMessageSound(context)
                lastReceivedMessage = latestMessage
            }

            onMessageReceived(newMessages)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.w("ChatScreen", "loadMessages:onCancelled", databaseError.toException())
        }
    }

    ref.addValueEventListener(messageListener)
}


fun sendMessage(pinId: String, messageText: String, context: Context) {
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
        SoundPlayer.playSendMessageSound(context)

        ref.push().setValue(chatMessage)
    }.addOnFailureListener {
        Log.e("ChatScreen", "Failed to fetch username", it)
    }
}
