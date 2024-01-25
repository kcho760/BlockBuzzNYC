// ProfileScreen.kt

package com.example.blockbuzznyc

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.blockbuzznyc.model.Achievement
import com.example.blockbuzznyc.model.MapPin
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class User(
    val userId: String = "",
    val username: String = "",
    val profilePictureUrl: String? = null, // Nullable if you want to allow users without a profile picture
    val numberOfPins: Int = 0,
    val totalLikes: Int = 0,
    val achievements: List<Achievement> = emptyList() // Add this line

)

@Composable
fun ProfileScreen(imageHandler: ImageHandler, onPinSelected: (MapPin) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var refreshToggle by remember { mutableStateOf(false) }
    val pins by getUserPins(userId).collectAsState(initial = emptyList())

    //pick image from gallery for profile pic
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadProfilePicture(userId, it,
                onSuccess = { imageUrl ->
                    profilePictureUrl = imageUrl
                    updateUserProfilePicture(userId, imageUrl) {
                        refreshToggle = !refreshToggle
                    }
                },
                onFailure = {
                    Log.d("ProfileScreen", "Failed to upload profile picture")
                }
            )
        }
    }

    // Define the userFlow outside LaunchedEffect
    val userFlow = flow {
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
        val user = userDoc.toObject(User::class.java) ?: User()
        emit(user)
    }
    LaunchedEffect(refreshToggle) {
        // Collect userFlow inside LaunchedEffect
        val user = userFlow.first()
        profilePictureUrl = user.profilePictureUrl
    }

    val user by userFlow.collectAsState(initial = User())

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(text = "Username: ${user.username}")

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Picture Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(16.dp)
                    .offset(x = (25).dp) // Adjust the position as needed
            ) {
                // Profile Picture Box, which is clickable to pick an image from the gallery
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") } // This will launch the image picker
                ) {
                    profilePictureUrl?.let { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } ?: run {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile Picture",
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
                // Camera Icon Button
                IconButton(
                    onClick = {
                        imageHandler.takePicture { uri ->
                            uri?.let {
                                uploadImageToFirebaseStorage(userId, it) { imageUrl ->
                                    updateUserProfilePicture(userId, imageUrl) {
                                        refreshToggle = !refreshToggle
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .offset(x = (-30).dp, y = 50.dp) // Adjust the position as needed
                        .zIndex(1f) // Make sure the icon is above the profile picture
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload Profile Picture",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(8.dp)
                    .background(Color.LightGray, CircleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Display the actual Pins count
                    CountSection(count = user.numberOfPins, label = "Pins")

                    // Display the actual Likes count
                    CountSection(
                        count = user.totalLikes,
                        label = "Likes"
                    ) // Updated to use totalLikes
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp)) // This will apply rounded corners with a radius of 10dp
                    .background(Color.LightGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow {
                        items(pins) { pin ->
                            PinCard(pin = pin, onPinSelected = {
                                Log.d("PinTracking", "Pin selected: ${pin.id}, Title: ${pin.title}")
                                onPinSelected(pin)
                            })
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp)) // This will apply rounded corners with a radius of 10dp
                    .background(Color.LightGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        user.achievements.let { achievements ->
                            achievements.forEach { achievement ->
                                if (achievement.earned) {
                                    Text(text = achievement.name) // Show each earned achievement
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
fun uploadImageToFirebaseStorage(userId: String, imageUri: Uri, onComplete: (String) -> Unit) {
    val storageRef = Firebase.storage.reference.child("profile_pictures/$userId.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                onComplete(uri.toString())
            }
        }
        .addOnFailureListener {
            // Handle any errors
        }
}
fun uploadProfilePicture(userId: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
    val storageRef = Firebase.storage.reference.child("profile_pictures/$userId.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
                fetchUserAndCheckAchievements(userId)
            }
                ?.addOnFailureListener {
                    // Could not get the download URL
                    onFailure()
                }
        }
        .addOnFailureListener {
            // Upload failed
            onFailure()
        }
}

fun updateUserProfilePicture(userId: String, profilePictureUrl: String, onSuccess: () -> Unit) {
    val userUpdates = mapOf("profilePictureUrl" to profilePictureUrl)
    FirebaseFirestore.getInstance().collection("users").document(userId).update(userUpdates)
        .addOnSuccessListener {
            onSuccess() // Notify that the profile picture update was successful
        }
        .addOnFailureListener {
            // Handle any failure in updating the Firestore document
        }
}

fun getUserPins(userId: String): Flow<List<MapPin>> = flow {
    val pins = FirebaseFirestore.getInstance().collection("pins")
        .whereEqualTo("creatorUserId", userId)
        .get()
        .await()
        .toObjects(MapPin::class.java)
    emit(pins)
}

@Composable
fun CountSection(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString())
        Text(text = label)
    }
}