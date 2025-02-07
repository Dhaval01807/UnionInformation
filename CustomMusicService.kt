package com.example.filesmanager.Service

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.filesmanager.Model.MusicModel
import com.example.filesmanager.R
import com.example.filesmanager.Utils.AppConstant
import com.example.filesmanager.Utils.Helper

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var binder = MusicBinder()
    val isPlaying: MutableLiveData<Boolean> = MutableLiveData(true)

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun getDuration(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun setDuration(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun pauseAndPlay() {
        if (mediaPlayer?.isPlaying!!) {
            mediaPlayer?.pause()
            isPlaying.postValue(false)
            notificationHandler(false, null)
        } else {
            mediaPlayer?.start()
            isPlaying.postValue(true)
            notificationHandler(true, null)
        }
    }

    override fun onCreate() {
        initializedMediaPlayer()
        super.onCreate()
    }

    fun initializedMediaPlayer() {
        val musicPiece = AppConstant.musicPlayList[AppConstant.musicIndex]
        mediaPlayer = MediaPlayer.create(this, ContentUris.withAppendedId(Helper.musicUri, musicPiece.id))
        mediaPlayer?.start()
        notificationHandler(true, musicPiece)
    }

    fun cleanAndRelease(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    fun notificationHandler(isPlayer: Boolean, musicPiece: MusicModel?) {
        val mediaSession = MediaSession(this, "PlayerService")
        val mediaStyle =
            Notification.MediaStyle().setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)

        val pauseAction: Notification.Action = Notification.Action.Builder(
            R.drawable.ic_pause_btn, "Pause", PendingIntent.getService(
                this,
                121,
                Intent(this, MusicService::class.java).apply {
                    action = "stop"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or FLAG_MUTABLE
            )
        ).build()

        val playAction: Notification.Action = Notification.Action.Builder(
            R.drawable.ic_play_btn, "Play", PendingIntent.getService(
                this,
                121,
                Intent(this, MusicService::class.java).apply {
                    action = "play"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or FLAG_MUTABLE
            )
        ).build()

        val nextAction = Notification.Action.Builder(
            R.drawable.ic_next_btn, "Next", PendingIntent.getService(
                this,
                121,
                Intent(this, MusicService::class.java).apply {
                    action = "next"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or FLAG_MUTABLE
            )
        ).build()
//
        val previousAction = Notification.Action.Builder(
            R.drawable.ic_previous_btn, "Previous", PendingIntent.getService(
                this,
                121,
                Intent(this, MusicService::class.java).apply {
                    action = "previous"
                },
                PendingIntent.FLAG_UPDATE_CURRENT or FLAG_MUTABLE
            )
        ).build()
        val notification = Notification.Builder(this, "12")
            .setStyle(mediaStyle)
            .setContentTitle("Music Player")
            .setSmallIcon(R.drawable.ic_apps)
            .setPriority(Notification.PRIORITY_MAX)
            .setWhen(0)
            .setVisibility(Notification.VISIBILITY_PUBLIC)

        notification.addAction(previousAction)

        if (isPlayer)
            notification.addAction(pauseAction)
        else
            notification.addAction(playAction)

        notification.addAction(nextAction)

        if (musicPiece != null) {
            mediaSession.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, musicPiece.fileName)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, musicPiece.artistName)
                    .putString(
                        MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                        ContentUris.withAppendedId(Helper.imageUri, 1000000055).toString()
                    )
                    .putLong(
                        MediaMetadata.METADATA_KEY_DURATION,
                        musicPiece.duration
                    )
                    .build()
            )
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setState(
                    if (isPlayer) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    mediaPlayer?.currentPosition!!.toLong(),
                    1F
                )
                .setActions(PlaybackState.ACTION_SEEK_TO)
                .build()
        )
        startForeground(121, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.e("TAG", "onStartCommand: $action")
        when (action) {
            "stop" -> pauseAndPlay()
            "play" -> pauseAndPlay()
            
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.e("TAG", "onDestroy: Destroy")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}
