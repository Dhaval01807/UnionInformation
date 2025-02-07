package com.example.filesmanager.Activity

import android.content.ContentUris
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.filesmanager.Interface.CustomMediaPlayer
import com.example.filesmanager.Layout.CustomMediaController
import com.example.filesmanager.R
import com.example.filesmanager.Utils.Helper
import com.example.filesmanager.databinding.ActivityVideoPlayerBinding

class VideoPlayer : AppCompatActivity(), CustomMediaPlayer {
    val TAG = "VideoPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var mediaController: CustomMediaController? = null
    private lateinit var binding: ActivityVideoPlayerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeMedia()
    }
    private fun initializeMedia(){
        binding.videoSurfaceContainer.setOnClickListener{
            mediaController!!.show()
        }
        binding.videoSurface.holder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.e(TAG, "onCreate: Surface Created")
                try{
                    mediaPlayer!!.setDisplay(holder)
                    mediaPlayer!!.prepare()
                }catch (e:Exception){
                    Log.e(TAG, "surfaceCreated: $e", )
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.reset()
        mediaPlayer!!.setOnPreparedListener(object : OnPreparedListener {
            override fun onPrepared(mp: MediaPlayer?) {
                try {
                    if(mediaPlayer!!.videoWidth >mediaPlayer!!.videoHeight){
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }

                    mediaController!!.setMediaPlayer(this@VideoPlayer);
                    mediaPlayer!!.start()
                    mediaController!!.setAnchorView(findViewById<ViewGroup>(R.id.videoSurfaceContainer)!!)
                    runLooper()
                }catch (e:Exception){
                    Log.e(TAG, "onPrepared: $e", )
                }
            }

        })
        mediaController = CustomMediaController(this)
        try {
            mediaPlayer!!.setDataSource(
                this,
                ContentUris.withAppendedId(Helper.videoUri, Helper.videoFolder[Helper.videoPosition].id)
            );
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: $e")
        }
    }

    fun runLooper(){


        var count:Int = 0
        val runner: Runnable =object: Runnable {
            override fun run() {
                count+=1
                Log.e(TAG, "run: Looper $count", )
                Handler(Looper.getMainLooper()).postDelayed(this,100)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed(runner ,0)


        Handler(Looper.getMainLooper()).postDelayed(runner,0)
    }

    override fun start() {
        mediaPlayer?.start()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun getDuration(): Int {
        return mediaPlayer?.duration ?: -1
    }

    override fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: -1
    }

    override fun seekTo(pos: Int) {
        mediaPlayer?.seekTo(pos)
    }

    override fun isPlaying(): Boolean {
        Log.e(TAG, "isPlaying: ${mediaPlayer?.isPlaying}", )
        return mediaPlayer?.isPlaying ?: false
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return false
    }

    override fun isFullScreen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toggleFullScreen() {

    }

    override fun nextItem() {
        mediaPlayer!!.release()
        recreate()
    }

    override fun prevItem() {
        mediaPlayer!!.release()
        recreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: ", )
        mediaController!!.finishAllJob()
        mediaPlayer?.release()
    }
}
