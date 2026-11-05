// [KATALOG] shared.ui.screens.player
package com.example.neetrust.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.neetrust.core.logger.AppLogger
import com.example.neetrust.core.util.FormatUtils
import com.example.neetrust.data.repository.AnimeRepository
import com.example.neetrust.domain.model.EpisodeStreamDto
import com.example.neetrust.domain.model.VideoServerDto
import com.example.neetrust.player.PlayerController
import com.example.neetrust.ui.config.AppConfig
import com.example.neetrust.ui.theme.MoriBgCard
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriBorder
import com.example.neetrust.ui.theme.MoriEmeraldDark
import com.example.neetrust.ui.theme.MoriEmeraldLight
import com.example.neetrust.ui.theme.MoriEmeraldPrimary
import com.example.neetrust.ui.theme.MoriTextInverse
import com.example.neetrust.ui.theme.MoriTextMuted
import com.example.neetrust.ui.theme.MoriTextPrimary
import com.example.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val streamDetail: EpisodeStreamDto? = null,
    val selectedServer: VideoServerDto? = null,
    val selectedQuality: String = "720p",
    val errorMessage: String? = null
)

class PlayerViewModel(
    val episodeSlug: String,
    private val animeRepository: AnimeRepository,
    val playerController: PlayerController
) : ViewModel() {
    private val logger = AppLogger.of("PlayerViewModel")

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadStream()
    }

    fun loadStream() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            logger.i("Loading episode stream for: $episodeSlug")
            val result = animeRepository.getEpisodeStream(episodeSlug)
            result.fold(
                onSuccess = { stream ->
                    val server = stream.servers.firstOrNull()
                    val savedPosSec = animeRepository.getSavedPosition(episodeSlug)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamDetail = stream,
                        selectedServer = server,
                        selectedQuality = server?.quality ?: "720p"
                    )

                    val urlToPlay = server?.streamUrl ?: stream.defaultStreamUrl ?: ""
                    if (urlToPlay.isNotBlank()) {
                        playerController.prepareAndPlay(
                            mediaUrl = urlToPlay,
                            startPositionMs = savedPosSec * 1000L,
                            serverName = server?.serverName ?: "Server Utama",
                            quality = server?.quality ?: "720p"
                        )
                    }
                },
                onFailure = { err ->
                    logger.e("Failed to load episode stream", err)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat streaming: ${err.message}"
                    )
                }
            )
        }
    }

    fun selectServer(server: VideoServerDto) {
        val stream = _uiState.value.streamDetail ?: return
        _uiState.value = _uiState.value.copy(selectedServer = server, selectedQuality = server.quality)
        val currentMs = playerController.state.value.currentPositionMs
        playerController.prepareAndPlay(
            mediaUrl = server.streamUrl,
            startPositionMs = currentMs,
            serverName = server.serverName,
            quality = server.quality
        )
    }

    fun selectQuality(quality: String) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
        playerController.updateServerAndQuality(
            serverName = _uiState.value.selectedServer?.serverName ?: "Server",
            quality = quality
        )
    }

    fun saveProgress() {
        val stream = _uiState.value.streamDetail ?: return
        val posMs = playerController.state.value.currentPositionMs
        val durMs = playerController.state.value.durationMs
        if (posMs > 1000) {
            viewModelScope.launch {
                animeRepository.saveWatchHistory(
                    animeSlug = stream.animeSlug,
                    animeTitle = stream.animeTitle ?: stream.animeSlug,
                    episodeSlug = stream.episodeSlug,
                    episodeNumber = stream.episodeNumber,
                    poster = "",
                    lastPositionSecs = posMs / 1000,
                    durationSecs = durMs / 1000
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        playerController.release()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEpisode: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playerController.state.collectAsState()
    val context = LocalContext.current
    var areControlsVisible by remember { mutableStateOf(true) }
    var showServerMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Auto-hide controls after 5 seconds of inactivity
    LaunchedEffect(areControlsVisible, playbackState.isPlaying) {
        if (areControlsVisible && playbackState.isPlaying) {
            delay(5000)
            areControlsVisible = false
        }
    }

    // Save progress periodically and on exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgress()
        }
    }

    BackHandler {
        viewModel.saveProgress()
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
            .testTag("player_screen")
    ) {
        // Media3 ExoPlayer Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.playerController.getPlayer()
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls
        AnimatedVisibility(
            visible = areControlsVisible || !playbackState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // TOP BAR CONTROLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.saveProgress()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = state.streamDetail?.title ?: "Memutar Video",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    // PiP Button
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context is Activity) {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                context.enterPictureInPictureMode(params)
                            }
                        },
                        modifier = Modifier.testTag("pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "Mode Gambar dalam Gambar (PiP)",
                            tint = MoriEmeraldLight
                        )
                    }
                }

                // CENTER CONTROLS: Rewind 10s, Play/Pause, Forward 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { viewModel.playerController.rewind10Seconds() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .testTag("player_rewind_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Mundur 10 Detik",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Play/Pause / Buffering Indicator
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MoriEmeraldDark.copy(alpha = 0.85f))
                            .clickable { viewModel.playerController.togglePlayPause() }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playbackState.isBuffering || state.isLoading) {
                            CircularProgressIndicator(
                                color = MoriEmeraldLight,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { viewModel.playerController.forward10Seconds() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .testTag("player_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Maju 10 Detik",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Error overlay with Retry & Change Server actions
                if (playbackState.error != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.90f))
                            .border(1.dp, MoriBorder, RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Kendala Pemutaran Video",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = playbackState.error ?: "Gagal memutar video",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoriTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showServerMenu = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MoriEmeraldPrimary,
                                        contentColor = MoriTextInverse
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pilih Server Lain")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.playerController.retry() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }

                // BOTTOM CONTROLS & TIMELINE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Seekbar Slider
                    val totalDuration = playbackState.durationMs.coerceAtLeast(1L)
                    val currentPos = playbackState.currentPositionMs.coerceIn(0L, totalDuration)

                    Slider(
                        value = currentPos.toFloat(),
                        onValueChange = { newPos ->
                            viewModel.playerController.seekTo(newPos.toLong())
                        },
                        valueRange = 0f..totalDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MoriEmeraldPrimary,
                            activeTrackColor = MoriEmeraldPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("player_timeline_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Duration Text
                        Text(
                            text = "${FormatUtils.formatMillis(currentPos)} / ${FormatUtils.formatMillis(totalDuration)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = Color.White
                        )

                        // Bottom Actions: Server, Quality, Subtitle, Prev/Next
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Prev Episode
                            state.streamDetail?.prevEpisodeSlug?.let { prevSlug ->
                                IconButton(
                                    onClick = {
                                        viewModel.saveProgress()
                                        onNavigateToEpisode(prevSlug)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Episode Sebelumnya", tint = Color.White)
                                }
                            }

                            // Next Episode
                            state.streamDetail?.nextEpisodeSlug?.let { nextSlug ->
                                IconButton(
                                    onClick = {
                                        viewModel.saveProgress()
                                        onNavigateToEpisode(nextSlug)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Episode Berikutnya", tint = Color.White)
                                }
                            }

                            // Subtitle Toggle
                            IconButton(
                                onClick = { viewModel.playerController.toggleSubtitle() },
                                modifier = Modifier.size(36.dp).testTag("toggle_subtitle_button")
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isSubtitleEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                    contentDescription = "Subtitle",
                                    tint = if (playbackState.isSubtitleEnabled) MoriEmeraldLight else MoriTextMuted
                                )
                            }

                            // Server Picker
                            Box {
                                IconButton(
                                    onClick = { showServerMenu = true },
                                    modifier = Modifier.size(36.dp).testTag("server_menu_button")
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = "Pilih Server", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showServerMenu,
                                    onDismissRequest = { showServerMenu = false },
                                    modifier = Modifier.background(MoriBgCard).border(1.dp, MoriBorder, RoundedCornerShape(8.dp))
                                ) {
                                    state.streamDetail?.servers?.forEach { server ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = server.serverName,
                                                    color = if (state.selectedServer?.serverId == server.serverId) MoriEmeraldLight else MoriTextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectServer(server)
                                                showServerMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quality Picker
                            Box {
                                IconButton(
                                    onClick = { showQualityMenu = true },
                                    modifier = Modifier.size(36.dp).testTag("quality_menu_button")
                                ) {
                                    Icon(Icons.Default.HighQuality, contentDescription = "Kualitas Video", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                    modifier = Modifier.background(MoriBgCard).border(1.dp, MoriBorder, RoundedCornerShape(8.dp))
                                ) {
                                    AppConfig.AVAILABLE_QUALITIES.forEach { q ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = q,
                                                    color = if (state.selectedQuality == q) MoriEmeraldLight else MoriTextPrimary
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectQuality(q)
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
