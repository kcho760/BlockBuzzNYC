// ProfileScreen.kt

package com.example.blockbuzznyc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class User(
    val userId: String = "",
    val username: String = "",
)

@Composable
fun ProfileScreen() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    // Fetch the user data from Firestore
    val userFlow = flow {
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
        val user = userDoc.toObject(User::class.java) ?: User()
        emit(user)
    }

    val user by userFlow.collectAsState(initial = User())

    Surface(modifier = Modifier
        .fillMaxSize()
    ) { // Surface with default background
        Column {
            Text(text = "Username: ${user.username}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}
