package com.example.blockbuzznyc

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
class ImageHandler(private val context: Context, private val activityResultCaller: ActivityResultCaller) {
    private var imageUri: Uri? = null
    private lateinit var onImageCaptured: ((Uri?) -> Unit)

    private val takePictureLauncher = activityResultCaller.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageCaptured(imageUri)
        } else {
            onImageCaptured(null)
        }
    }

    fun takePicture(onImageCaptured: (Uri?) -> Unit) {
        this.onImageCaptured = onImageCaptured
        imageUri = createImageFileUri()  // Call to create a new image file
        takePictureLauncher.launch(imageUri)
    }

    private fun createImageFileUri(): Uri {
        val photoFile = createImageFile()  // Call the method to create the file
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }
}
