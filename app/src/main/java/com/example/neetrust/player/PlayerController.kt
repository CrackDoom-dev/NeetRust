// [KATALOG] shared.player.PlayerController
package com.example.neetrust.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.neetrust.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isEnded: Boolean = false,
    val error: String? = null,
    val isSubtitleEnabled: Boolean = true,
    val selectedServerName: String = "Server Utama",
    val selectedQuality: String = "720p"
)

class PlayerController(private val context: Context) {
    private val logger = AppLogger.of("PlayerController")
    private var exoPlayer: ExoPlayer? = null

    private val _state = MutableStateFlow(PlayerPlaybackState())
    val state: StateFlow<PlayerPlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var tickerJob: Job? = null

    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            initPlayer()
        }
        return exoPlayer!!
    }

    private var lastMediaUrl: String? = null
    private var lastStartPositionMs: Long = 0L

    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        logger.i("Initializing Media3 ExoPlayer instance")
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; NeetRust) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _state.value = _state.value.copy(isBuffering = true, isEnded = false)
                    }
                    Player.STATE_READY -> {
                        _state.value = _state.value.copy(
                            isBuffering = false,
                            durationMs = player.duration.coerceAtLeast(0L),
                            isEnded = false,
                            error = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        _state.value = _state.value.copy(
                            isPlaying = false,
                            isBuffering = false,
                            isEnded = true
                        )
                    }
                    Player.STATE_IDLE -> {
                        _state.value = _state.value.copy(isBuffering = false)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                logger.e("ExoPlayer error: ${error.errorCodeName}", error)
                _state.value = _state.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    error = "Gagal memutar video: ${error.message ?: "Format atau server tidak didukung"}"
                )
            }
        })

        exoPlayer = player
        startPositionTicker()
    }

    fun prepareAndPlay(mediaUrl: String, startPositionMs: Long = 0L, serverName: String = "Server Utama", quality: String = "720p") {
        logger.i("Preparing playback: $mediaUrl (startPos: ${startPositionMs}ms, server: $serverName)")
        lastMediaUrl = mediaUrl
        lastStartPositionMs = startPositionMs
        val player = getPlayer()
        _state.value = _state.value.copy(
            selectedServerName = serverName,
            selectedQuality = quality,
            error = null,
            isBuffering = true
        )

        val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
        player.setMediaItem(mediaItem)
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
        player.prepare()
        player.playWhenReady = true
    }

    fun retry() {
        val url = lastMediaUrl ?: return
        val pos = _state.value.currentPositionMs.takeIf { it > 0 } ?: lastStartPositionMs
        prepareAndPlay(url, pos, _state.value.selectedServerName, _state.value.selectedQuality)
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun forward10Seconds() {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + 10000).coerceAtMost(player.duration)
            player.seekTo(target)
        }
    }

    fun rewind10Seconds() {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - 10000).coerceAtLeast(0L)
            player.seekTo(target)
        }
    }

    fun toggleSubtitle() {
        val current = _state.value.isSubtitleEnabled
        _state.value = _state.value.copy(isSubtitleEnabled = !current)
    }

    fun updateServerAndQuality(serverName: String, quality: String) {
        _state.value = _state.value.copy(
            selectedServerName = serverName,
            selectedQuality = quality
        )
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.playbackState == Player.STATE_READY || player.isPlaying) {
                        _state.value = _state.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L),
                            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    fun release() {
        logger.i("Releasing ExoPlayer")
        tickerJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
