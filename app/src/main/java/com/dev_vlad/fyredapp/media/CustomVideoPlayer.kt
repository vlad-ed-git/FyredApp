package com.dev_vlad.fyredapp.media

import android.net.Uri
import android.widget.Toast
import com.dev_vlad.fyredapp.R
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Log
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
                    Log.d(
                        LOG_TAG,
                        "from fyredApp | onPlayerError TYPE_SOURCE: ${error.sourceException.message}"
                    )
                }
                ExoPlaybackException.TYPE_RENDERER -> Log.d(
                    LOG_TAG,
                    "from fyredApp | onPlayerError TYPE_RENDERER: ${error.rendererException.message}"
                )
                ExoPlaybackException.TYPE_UNEXPECTED -> Log.d(
                    LOG_TAG,
                    "from fyredApp | onPlayerError TYPE_UNEXPECTED: ${error.unexpectedException.message}"
                )

                else -> {
                    Log.d(
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
        exoPlayer = SimpleExoPlayer.Builder(context).build()
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
            Log.d(LOG_TAG, "from fyredApp | playing video")
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
        Log.d(LOG_TAG, "from fyredApp | video player released")
    }


}