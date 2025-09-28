package com.bsikar.helix.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.bsikar.helix.MainActivity
import com.bsikar.helix.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background playback service for audiobooks
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    
    @Inject
    lateinit var audioBookPlayer: AudioBookPlayer
    
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    
    inner class PlaybackServiceBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
        fun getAudioBookPlayer(): AudioBookPlayer = audioBookPlayer
    }
    
    private val binder = PlaybackServiceBinder()
    
    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        setupNotification()
    }
    
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(MediaSessionCallback())
            .build()
    }
    
    private fun setupNotification() {
        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.app_name)
            .setChannelDescriptionResourceId(R.string.app_name)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return audioBookPlayer.playbackState.value.currentBook?.title ?: "Audiobook"
                }
                
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(this@PlaybackService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    return PendingIntent.getActivity(
                        this@PlaybackService,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }
                
                override fun getCurrentContentText(player: Player): CharSequence {
                    return audioBookPlayer.playbackState.value.currentChapter?.title ?: ""
                }
                
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): android.graphics.Bitmap? {
                    // TODO: Load cover art bitmap
                    return null
                }
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: android.app.Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
                
                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            })
            .build()
        
        notificationManager?.setPlayer(exoPlayer)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        notificationManager?.setPlayer(null)
        super.onDestroy()
    }
    
    private inner class MediaSessionCallback : MediaSession.Callback {
        // Media session callbacks will be implemented as needed
        // For now, we'll keep this minimal to ensure compilation
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "audiobook_playback_channel"
    }
}