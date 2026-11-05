package com.neetrust.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.neetrust.core.logger.AppLogger
import com.neetrust.data.repository.AnimeRepository
import com.neetrust.domain.model.AnimeDetailDto
import com.neetrust.ui.components.EpisodeItem
import com.neetrust.ui.components.ErrorState
import com.neetrust.ui.components.LoadingState
import com.neetrust.ui.components.NeetRustTopBar
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.theme.MoriAmberAccent
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriBgSurface
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriEmeraldDark
import com.neetrust.ui.theme.MoriEmeraldLight
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextInverse
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
import com.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: AnimeDetailDto? = null,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null
)

class DetailViewModel(
    private val animeSlug: String,
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val logger = AppLogger.of("DetailViewModel")

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            logger.i("Loading anime detail for slug: $animeSlug")
            val isFav = animeRepository.isFavorite(animeSlug)
            val result = animeRepository.getAnimeDetail(animeSlug)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = detail,
                        isFavorite = isFav
                    )
                },
                onFailure = { err ->
                    logger.e("Failed to load anime detail", err)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat detail anime: ${err.message}"
                    )
                }
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val detail = _uiState.value.detail ?: return@launch
            val currentlyFav = _uiState.value.isFavorite
            if (currentlyFav) {
                logger.i("Removing from favorites: $animeSlug")
                animeRepository.removeFavorite(animeSlug)
                _uiState.value = _uiState.value.copy(isFavorite = false)
            } else {
                logger.i("Adding to favorites: $animeSlug")
                animeRepository.addFavorite(
                    animeSlug = animeSlug,
                    title = detail.title,
                    poster = detail.poster,
                    score = detail.score ?: "N/A",
                    status = detail.status ?: "Ongoing"
                )
                _uiState.value = _uiState.value.copy(isFavorite = true)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onEpisodeClick: (episodeSlug: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = state.detail?.title ?: "Detail Anime",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (state.detail != null) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier.testTag("detail_favorite_toggle")
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (state.isFavorite) "Hapus Favorit" else "Tambah Favorit",
                                tint = if (state.isFavorite) MoriAmberAccent else MoriEmeraldPrimary
                            )
                        }
                    }
                }
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("detail_screen")
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingState(
                    message = "Memuat detail & episode...",
                    modifier = Modifier.padding(padding)
                )
            }
            state.errorMessage != null && state.detail == null -> {
                ErrorState(
                    message = state.errorMessage!!,
                    onRetry = { viewModel.loadDetail() },
                    modifier = Modifier.padding(padding)
                )
            }
            state.detail != null -> {
                val detail = state.detail!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(0.70f)
                                    .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                                    .background(MoriBgCard)
                                    .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                            ) {
                                AsyncImage(
                                    model = detail.poster,
                                    contentDescription = detail.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                    color = MoriTextPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MoriAmberAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = detail.score ?: "N/A",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MoriAmberAccent
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${detail.status} • ${detail.type}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MoriTextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (detail.episodes.isNotEmpty()) {
                                    Button(
                                        onClick = { onEpisodeClick(detail.episodes.last().slug) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MoriEmeraldPrimary,
                                            contentColor = MoriTextInverse
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("play_first_episode_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Putar Ep 1", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        item {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MoriBgSurface)
                                            .border(1.dp, MoriBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MoriEmeraldLight
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Sinopsis",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MoriTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detail.synopsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoriTextSecondary,
                                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isSynopsisExpanded) "Tampilkan Sedikit" else "Selengkapnya...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MoriEmeraldPrimary,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Episode (${detail.episodes.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MoriTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(detail.episodes) { ep ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            EpisodeItem(
                                title = ep.title,
                                episodeNumber = ep.number,
                                uploadedAt = ep.uploadedAt,
                                onClick = { onEpisodeClick(ep.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}
