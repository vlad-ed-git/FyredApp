package com.dev_vlad.fyredapp.media

import android.net.Uri
import android.widget.Toast
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.utils.AppConstants
import com.dev_vlad.fyredapp.utils.MyLog
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class CustomVideoPlayer(
    private val momentVideoPv: PlayerView,
    private val videoUriStr: String
) {
    companion object {
        private val LOG_TAG = CustomVideoPlayer::class.java.simpleName
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSource: MediaSource? = null

    /********LISTENER *********/
    private val playerListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            super.onPlayerError(error)
            when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> {
                    Toast.makeText(momentVideoPv.context, R.string.err_internet, Toast.LENGTH_LONG)
                        .show()
                    MyLog.d(
                        LOG_TAG,
                        "from fyredApp | onPlayerError TYPE_SOURCE: ${error.sourceException.message}"
                    )
                }
                ExoPlaybackException.TYPE_RENDERER -> MyLog.d(
                    LOG_TAG,
                    "from fyredApp | onPlayerError TYPE_RENDERER: ${error.rendererException.message}"
                )
                ExoPlaybackException.TYPE_UNEXPECTED -> MyLog.d(
                    LOG_TAG,
                    "from fyredApp | onPlayerError TYPE_UNEXPECTED: ${error.unexpectedException.message}"
                )

                else -> {
                    MyLog.d(
                        LOG_TAG,
                        "from fyredApp | an unknown error occured -> probably no internet"
                    )
                }
            }
        }

    }

    init {
        initializePlayer()
    }

    /******************VIDEO PLAYING ********************/
    private fun initializePlayer() {
        val context = momentVideoPv.context
        val builder = DefaultLoadControl.Builder()
        /* Milliseconds of media data buffered before playback starts or resumes. */
        val minBufferToPlayMs = 1000
        val maxBufferMs = AppConstants.MAX_VIDEO_SECONDS * 1000
        val minBufferMs = (maxBufferMs / 2)
        builder.setBufferDurationsMs(
            minBufferMs,
            maxBufferMs,
            minBufferToPlayMs,
            minBufferMs
        )
        val loadControl = builder.createDefaultLoadControl()
        exoPlayer = SimpleExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
        exoPlayer?.let { nonNullExoPlayer ->
            nonNullExoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            momentVideoPv.player = nonNullExoPlayer
            momentVideoPv.hideController()
            momentVideoPv.useController = false
            momentVideoPv.controllerHideOnTouch = true
            val userAgent =
                Util.getUserAgent(
                    context,
                    context.getString(R.string.app_name)
                )
            mediaSource = ProgressiveMediaSource
                .Factory(
                    DefaultDataSourceFactory(context, userAgent),
                    DefaultExtractorsFactory()
                )
                .createMediaSource(Uri.parse(videoUriStr))


            nonNullExoPlayer.prepare(mediaSource!!, true, false)
            nonNullExoPlayer.addListener(playerListener)
            nonNullExoPlayer.playWhenReady = true
            MyLog.d(LOG_TAG, "from fyredApp | playing video")
        }
    }

    fun releasePlayer() {
        exoPlayer?.let {
            it.stop()
            it.release()
            it.removeListener(playerListener)

        }
        exoPlayer = null
        mediaSource = null
        MyLog.d(LOG_TAG, "from fyredApp | video player released")
    }


}