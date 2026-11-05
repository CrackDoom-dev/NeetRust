// [KATALOG] shared.ui.screens.history
package com.example.neetrust.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.neetrust.core.logger.AppLogger
import com.example.neetrust.core.util.FormatUtils
import com.example.neetrust.data.repository.AnimeRepository
import com.example.neetrust.domain.model.HistoryItemDto
import com.example.neetrust.ui.components.NeetRustTopBar
import com.example.neetrust.ui.config.AppConfig
import com.example.neetrust.ui.theme.MoriBgCard
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriBorder
import com.example.neetrust.ui.theme.MoriCoralAlert
import com.example.neetrust.ui.theme.MoriEmeraldDark
import com.example.neetrust.ui.theme.MoriEmeraldLight
import com.example.neetrust.ui.theme.MoriEmeraldPrimary
import com.example.neetrust.ui.theme.MoriTextInverse
import com.example.neetrust.ui.theme.MoriTextMuted
import com.example.neetrust.ui.theme.MoriTextPrimary
import com.example.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val logger = AppLogger.of("HistoryViewModel")
    val history: StateFlow<List<HistoryItemDto>> = animeRepository.history

    fun loadHistory() {
        viewModelScope.launch {
            logger.i("Refreshing watch history")
            animeRepository.refreshHistory()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            logger.i("Clearing all watch history")
            animeRepository.clearHistory()
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onEpisodeClick: (episodeSlug: String) -> Unit,
    onBrowseClick: () -> Unit
) {
    val history by viewModel.history.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Riwayat Tontonan",
                canNavigateBack = false,
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllHistory() },
                            modifier = Modifier.testTag("clear_history_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Bersihkan Riwayat",
                                tint = MoriCoralAlert
                            )
                        }
                    }
                }
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("history_screen")
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MoriTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Belum Ada Riwayat",
                        style = MaterialTheme.typography.titleLarge,
                        color = MoriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Episode yang telah Anda tonton akan otomatis tercatat dan tersimpan posisinya di sini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoriTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBrowseClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoriEmeraldPrimary,
                            contentColor = MoriTextInverse
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mulai Menonton")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history, key = { it.episodeSlug }) { item ->
                    HistoryCardItem(
                        item = item,
                        onClick = { onEpisodeClick(item.episodeSlug) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCardItem(
    item: HistoryItemDto,
    onClick: () -> Unit
) {
    val progress = if (item.durationSeconds > 0) {
        (item.lastPositionSeconds.toFloat() / item.durationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .background(MoriBgCard)
            .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(85.dp)
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MoriBgDeep)
            ) {
                if (item.poster.isNotBlank()) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = item.animeTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MoriEmeraldLight)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MoriEmeraldDark.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "EP ${item.episodeNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = MoriEmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.animeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = MoriTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Episode ${item.episodeNumber}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MoriEmeraldLight
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Posisi: ${FormatUtils.formatDuration(item.lastPositionSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MoriTextMuted
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoriEmeraldDark,
                    contentColor = MoriEmeraldLight
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Lanjut", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (progress > 0f) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MoriEmeraldPrimary,
                trackColor = MoriBgDeep
            )
        }
    }
}
