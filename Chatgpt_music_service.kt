import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private val songs = listOf(
        "https://example.com/song1.mp3",
        "https://example.com/song2.mp3"
    )
    private var currentSongIndex = 0

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MusicService")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initMediaSession()
    }

    private fun initMediaSession() {
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                playMusic()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onPause() {
                pauseMusic()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }

            override fun onSkipToNext() {
                nextSong()
            }

            override fun onSkipToPrevious() {
                prevSong()
            }
        })

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                              MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
    }

    private fun playMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(songs[currentSongIndex])
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener { nextSong() }
            }
        } else {
            mediaPlayer?.start()
        }
        startForeground(1, buildNotification())
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        stopForeground(false)
    }

    private fun nextSong() {
        currentSongIndex = (currentSongIndex + 1) % songs.size
        mediaPlayer?.release()
        mediaPlayer = null
        playMusic()
    }

    private fun prevSong() {
        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else songs.size - 1
        mediaPlayer?.release()
        mediaPlayer = null
        playMusic()
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun buildNotification(): Notification {
        val playPauseAction =
            if (mediaPlayer?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, "music_channel")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Playing Music")
            .setContentText("Now playing: Song ${currentSongIndex + 1}")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(NotificationCompat.Action(playPauseAction, "Play/Pause", getActionIntent("PLAY_PAUSE")))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", getActionIntent("NEXT")))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", getActionIntent("PREV")))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .build()
    }

    private fun getActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY_PAUSE" -> if (mediaPlayer?.isPlaying == true) pauseMusic() else playMusic()
            "NEXT" -> nextSong()
            "PREV" -> prevSong()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
