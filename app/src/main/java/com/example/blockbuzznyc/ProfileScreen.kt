// ProfileScreen.kt

package com.example.blockbuzznyc

import android.content.Context
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current

    //pick image from gallery for profile pic
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadProfilePicture(
                userId = userId,
                imageUri = it,
                context = context, // Pass the context here
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

    val userFlow = flow {
        // Ensure that we have a valid userId before trying to read from Firestore
        if (userId.isNotEmpty()) {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: User()
            emit(user)
        } else {
            emit(User()) // Emit an empty User object or handle the error as appropriate
        }
    }
    LaunchedEffect(refreshToggle) {
        val user = userFlow.first()
        profilePictureUrl = user.profilePictureUrl
    }

    val user by userFlow.collectAsState(initial = User())

    Surface(modifier = Modifier
        .background(MaterialTheme.colorScheme.background)
        .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Username
            Text(
                text = user.username,
                color = MaterialTheme.colorScheme.tertiary, // This sets the text color to your primary color
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.25f,
                )
            )


//            Spacer(modifier = Modifier.height(16.dp))

            // Profile Picture Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
                    .offset(x = (25).dp) // Adjust the position as needed
            ) {
                // Profile Picture Box, which is clickable to pick an image from the gallery
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Black, CircleShape)
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
                        .border(2.dp, Color.Black, CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload Profile Picture",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            //section for counts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(2.dp)
                    .border(1.dp, Color.Black, RoundedCornerShape(41.dp))
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Display the actual Pins count
                    CountSection(count = user.numberOfPins, label = "Created Pins")

                    // Display the actual Likes count
                    CountSection(
                        count = user.totalLikes,
                        label = "Likes"
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp)) // Apply rounded corners to the outer box
                    .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Title Box for "Pins" text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth() // Make the title box stretch to the sides of the outer box
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) // Rounded corners for the title box
                            .background(MaterialTheme.colorScheme.tertiary) // Background color for the title box
                            .padding(8.dp) // Padding inside the title box
                    ) {
                        Text(
                            text = "Pins",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier
                                .padding(vertical = 8.dp) // Vertical padding for the text
                                .fillMaxWidth() // Ensure the Text fills the width
                                .wrapContentWidth(Alignment.CenterHorizontally) // Center the text horizontally within the Text composable
                        )
                    }

                    // Pins content
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp), // Padding for the LazyRow content
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                    .clip(RoundedCornerShape(20.dp)) // Apply rounded corners to the outer box
                    .height(200.dp)
                    .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The Box for the "Achievements" Text with matching rounded corners on the top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth() // Fill the width of the parent Column
                            .background(MaterialTheme.colorScheme.tertiary) // Background color of the Box
                            .padding(8.dp) // Padding inside the Box around the text
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)), // Apply clip after background to ensure corners are rounded
                        contentAlignment = Alignment.Center // Center the Text inside the Box
                    ) {
                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.titleMedium, // Apply a medium title style
                            color = MaterialTheme.colorScheme.onTertiary, // Text color for better contrast
                            modifier = Modifier.padding(8.dp) // Padding for the Text inside the Box
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Space between title and items
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(user.achievements.filter { it.earned }) { achievement ->
                            AchievementItem(achievement)
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
fun uploadProfilePicture(
    userId: String,
    imageUri: Uri,
    context: Context, // Add this parameter
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {
    val storageRef = Firebase.storage.reference.child("profile_pictures/$userId.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
                fetchUserAndCheckAchievements(userId, context)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(2.dp)) // To match your design
            .background(MaterialTheme.colorScheme.tertiary) // Your themed surface color
    ) {
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onTertiary // Ensure there is contrast
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onTertiary // Ensure there is contrast
        )
    }
}


@Composable
fun AchievementItem(achievement: Achievement) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp) // Padding for each item
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp)) // Border for each item
            .size(width = 150.dp, height = 110.dp)
            .background(MaterialTheme.colorScheme.background), // Define the size of the card
        shape = RoundedCornerShape(8.dp), // Rounded corners for the card
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondary), // Background color for the card
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize() // Fill the entire Card with the Column
                    .padding(8.dp), // Padding inside the Column
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.trophy_icon),
                    contentDescription = "Achievement",
                    modifier = Modifier
                        .size(45.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = Color.Unspecified // This will keep the original drawable colors
                )
                Spacer(modifier = Modifier.height(4.dp)) // Space between icon and text
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyMedium, // Use bodySmall or any other available style
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(4.dp)) // Space between icon and text
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall, // Use bodySmall or any other available style
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
    )
}
