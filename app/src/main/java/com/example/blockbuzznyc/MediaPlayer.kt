package com.example.blockbuzznyc

// SoundPlayer.kt
import android.content.Context
import android.media.MediaPlayer

object SoundPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun playChime(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.click_sound)
        }
        mediaPlayer?.start()
    }

    fun playSendMessageSound(context: Context) {
        // Ensure you have a send sound in your raw folder
        mediaPlayer = MediaPlayer.create(context, R.raw.sent_message)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }

    fun playReceiveMessageSound(context: Context) {
        // Ensure you have a receive sound in your raw folder
        mediaPlayer = MediaPlayer.create(context, R.raw.incoming_message)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
