package com.example.filesmanager.Layout

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.filesmanager.Interface.CustomMediaPlayer
import com.example.filesmanager.R
import com.example.filesmanager.Utils.Helper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CustomMediaController(private val context: Context) : FrameLayout(context) {


    private final val TAG = "CustomMediaController"
    private var anchorView: ViewGroup? = null
    private var mediaPlayer: CustomMediaPlayer? = null
    private var controllerView: View? = null
    private var controllerShowing: Boolean = false;

    private  var showingThread: Job? = null

    // Buttons And Layouts
    private lateinit var playAndPauseBtn: ImageView
    private lateinit var nextVideoBtn: ImageView
    private lateinit var previousVideoBtn: ImageView
    private lateinit var slider: SeekBar
    private lateinit var controllerLayout: ConstraintLayout

    private lateinit var currentDuration: TextView
    private lateinit var totalDuration: TextView


    fun setAnchorView(view: ViewGroup) {
        Log.e(TAG, "setAnchorView: Called")
        anchorView = view

        val frameParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        removeAllViews()

        val controllerView = getControllerView()
        addView(controllerView, frameParams)
    }

    fun getControllerView(): View {
        val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        controllerView = layoutInflater.inflate(R.layout.media_controller_layout, null)
        initializeButtons()
        Log.e(TAG, "getControllerView: Controller Is Not Null")
        checkDuration()
        return controllerView!!
    }

    fun initializeButtons() {
        val frameParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        playAndPauseBtn = controllerView!!.findViewById<ImageView>(R.id.iv_video_play_pause)
        nextVideoBtn = controllerView!!.findViewById<ImageView>(R.id.iv_video_next)
        previousVideoBtn = controllerView!!.findViewById<ImageView>(R.id.iv_video_previous)
        controllerLayout = controllerView!!.findViewById<ConstraintLayout>(R.id.controller_layout)
        currentDuration = controllerView!!.findViewById(R.id.tv_current_duration)
        totalDuration = controllerView!!.findViewById(R.id.tv_total_duration)

        slider = controllerView!!.findViewById<SeekBar>(R.id.sb_video_progress)

        slider.max = mediaPlayer!!.getDuration()
        slider.progress = mediaPlayer!!.getCurrentPosition()

        if (mediaPlayer!!.isPlaying()) {
            playAndPauseBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_pause_btn))
        } else {
            playAndPauseBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_play_btn))

        }

        initializeListener()

        frameParams.gravity = Gravity.BOTTOM
        hide()
        anchorView!!.addView(this, frameParams)
    }


    fun initializeListener() {
        slider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mediaPlayer?.seekTo(seekBar!!.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        nextVideoBtn.setOnClickListener {
            Helper.videoPosition =
                if (Helper.videoPosition < Helper.videoFolder.size - 1) Helper.videoPosition + 1 else 0

            mediaPlayer!!.nextItem()
        }

        previousVideoBtn.setOnClickListener {
            Helper.videoPosition =
                if (0 < Helper.videoPosition) Helper.videoPosition - 1 else Helper.videoFolder.size - 1
            mediaPlayer!!.prevItem()
        }

        playAndPauseBtn.setOnClickListener {
            updatePlayAndPause()
        }
    }

    private fun updatePlayAndPause() {
        val playAndPauseBtn = controllerView!!.findViewById<ImageView>(R.id.iv_video_play_pause)
        if (mediaPlayer!!.isPlaying()) {
            mediaPlayer?.pause()
            playAndPauseBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_play_btn))

        } else {
            mediaPlayer?.start()
            playAndPauseBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_pause_btn))
        }
    }

    fun show(timeOut: Long) {
        showingThread = CoroutineScope(Dispatchers.Main).launch {
            repeat(3, {
                Log.e(TAG, "checkDuration: Loop")
                 slider.progress = mediaPlayer!!.getCurrentPosition()
                val current = Helper.getDuration(mediaPlayer!!.getCurrentPosition().toLong())
                currentDuration.text = current
                delay(1000)
            })
            hide()
        }
        showingThread?.start()


    }

    fun show() {
        if (!controllerShowing) {
            show(3)
            controllerLayout.visibility = VISIBLE
            controllerShowing = true
        } else {
            if (showingThread?.isActive!!) {
                showingThread?.cancel()
            }
            show(3)
        }
    }

    fun hide() {
        controllerShowing = false
        controllerLayout.visibility = INVISIBLE
    }

    fun setMediaPlayer(mediaPlayer: CustomMediaPlayer) {
        this.mediaPlayer = mediaPlayer
    }


    fun finishAllJob() {

        Log.e(TAG, "finalize: ")

        if (showingThread != null) {
            if (showingThread?.isActive!!) {
                showingThread?.cancel()
            }
        }
    }

    fun checkDuration() {
        totalDuration.text = Helper.getDuration(mediaPlayer!!.getDuration().toLong())
        slider.max = mediaPlayer!!.getDuration()
//        Log.e(TAG, "checkDuration: ${mediaPlayer!!.getDuration()}")
//
//        CoroutineScope(Dispatchers.Main).launch {
//            while (mediaPlayer!!.isPlaying()) {
//                Log.e(TAG, "checkDuration: Loop", )
//                slider.progress = mediaPlayer!!.getCurrentPosition()
//                val current = AppConstant.getDuration(mediaPlayer!!.getCurrentPosition().toLong())
//                currentDuration.text = current
//                delay(1000)
//            }
//        }
//        val durationRunner =object : Runnable {
//            override fun run() {
//                Log.e(TAG, "checkDuration: ${mediaPlayer!!.getDuration()}", )
//                if(mediaPlayer!!.isPlaying()){
//                    slider.progress = mediaPlayer!!.getCurrentPosition()
//                    val current = AppConstant.getDuration(mediaPlayer!!.getCurrentPosition().toLong())
//                    currentDuration.text = current
//                }else{
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        this
//                    },1000)
//                }
//            }
//
//        }

    }
}
