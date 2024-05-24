package com.example.blockbuzznyc

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TopAppBarDefaults
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
    var activeUserCount by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pinId) {
        listenForMessages(pinId, context) { newMessages ->
            messages = newMessages
        }
    }

    LaunchedEffect(pinId) {
        trackUserPresence(pinId)
    }

    LaunchedEffect(pinId) {
        listenForActiveUsers(pinId) { count ->
            activeUserCount = count
            Log.d("ChatScreen", "Active user count updated: $activeUserCount")
        }
    }

    // Log whenever activeUserCount changes
    LaunchedEffect(activeUserCount) {
        Log.d("ChatScreen", "Active users text should be displayed with count: $activeUserCount")
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = pinTitle)
                                Text(
                                        text = "Active users: $activeUserCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors = topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                        )
                )
            }
    ) { innerPadding ->
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .imePadding(),
                        state = listState,
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
                                            .padding(10.dp)
                            )
                        }
                    }
                }

                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
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
                    if (listState.firstVisibleItemIndex < messages.size - 1) {
                        IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                },
                        ) {
                            Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Scroll to bottom",
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Button(
                            onClick = {
                                sendMessage(pinId, messageText, context)
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
fun listenForMessages(pinId: String, context: Context, onMessageReceived: (List<ChatMessage>) -> Unit) {
    val ref = Firebase.database.reference.child("chats/$pinId")
    val messageListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val newMessages = dataSnapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
            onMessageReceived(newMessages)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.w("ChatScreen", "loadMessages:onCancelled", databaseError.toException())
        }
    }
    ref.addValueEventListener(messageListener)
}

fun trackUserPresence(pinId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val ref = Firebase.database.reference.child("chats/$pinId/activeUsers/${currentUser.uid}")
    ref.onDisconnect().removeValue()
    ref.setValue(true)
    Log.d("ChatScreen", "User ${currentUser.uid} is now active in chat $pinId")
}

fun listenForActiveUsers(pinId: String, onActiveUsersChanged: (Int) -> Unit) {
    val ref = Firebase.database.reference.child("chats/$pinId/activeUsers")
    ref.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val activeUsers = dataSnapshot.childrenCount.toInt()
            onActiveUsersChanged(activeUsers)
            Log.d("ChatScreen", "Active users in chat $pinId: $activeUsers")
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.w("ChatScreen", "listenForActiveUsers:onCancelled", databaseError.toException())
        }
    })
}
