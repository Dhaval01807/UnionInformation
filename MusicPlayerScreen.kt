package com.example.filesmanager.Activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.filesmanager.R
import com.example.filesmanager.Service.MusicService
import com.example.filesmanager.Utils.AppConstant
import com.example.filesmanager.Utils.Helper
import com.example.filesmanager.databinding.ActivityMusicPlayerScreenBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicPlayerScreen : AppCompatActivity() {
    private lateinit var binding: ActivityMusicPlayerScreenBinding

    private lateinit var mService: MusicService
    private var mBound: Boolean = false

    private var progressRunnable: Job?=null
    private var totalDuration: Long =0

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            mService = binder.getService()
            startLookForDuration()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeMusicValues()
    }

    //Like Music Name And Artist, Duration And Other
    fun initializeMusicValues() {
        val musicPiece = AppConstant.musicPlayList[AppConstant.musicIndex]
        binding.tvMusicName.text = musicPiece.fileName
        binding.tvArtistName.text = musicPiece.artistName
        binding.tvMusicTotalDuration.text = Helper.getDuration(musicPiece.duration)
        binding.tvMusicProgressDuration.text = "0:12"
        binding.sbVideoProgress.max = musicPiece.duration.toInt()
        binding.sbVideoProgress.progress = 0

        if(mBound){
            if(progressRunnable!=null){
                progressRunnable!!.cancel()
            }
            mService.cleanAndRelease()
            mService.initializedMediaPlayer()
        }else{

            Intent(this, MusicService::class.java).also { intent ->
                bindService(intent, connection, BIND_AUTO_CREATE)
            }
        }
        totalDuration=musicPiece.duration
        initializeAllListener()
    }

    private fun initializeAllListener() {
        binding.ivMusicPlay.setOnClickListener {
            val playOrPause = mService.pauseAndPlay()
        }

        binding.ivMusicNext.setOnClickListener {
            if(AppConstant.musicIndex==AppConstant.musicPlayList.size-1){
                AppConstant.musicIndex = 0
            }else if(AppConstant.musicIndex<AppConstant.musicPlayList.size){
                AppConstant.musicIndex+=1
            }

            initializeMusicValues()
        }
        binding.ivMusicPrevious.setOnClickListener {
            if(AppConstant.musicIndex==0){
                AppConstant.musicIndex = AppConstant.musicPlayList.size-1
            }else{
                AppConstant.musicIndex-=1
            }
            initializeMusicValues()
        }

        binding.sbVideoProgress.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    binding.tvMusicProgressDuration.text = Helper.getDuration(p1.toLong())

                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    mService.setDuration(p0!!.progress)
                }

            },
        )
    }

    fun startLookForDuration(){
        mService.isPlaying.observe(this){
            if(it){
                binding.ivMusicPlay.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_pause_btn))
            }else{
                binding.ivMusicPlay.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_play_btn))
            }
        }
        Log.e("TAG", "startLookForDuration: ", )
        progressRunnable =CoroutineScope(Dispatchers.IO).launch {
            do{
                val duration = mService.getDuration()
                Log.e("TAG", "startLookForDuration: $duration", )
                binding.sbVideoProgress.progress = duration
                delay(1000)
            }while (duration<totalDuration)
        }
    }

    override fun onDestroy() {
        if(progressRunnable!=null){
            progressRunnable!!.cancel()
        }
        unbindService(connection)
        mBound = false
        super.onDestroy()
    }
}
