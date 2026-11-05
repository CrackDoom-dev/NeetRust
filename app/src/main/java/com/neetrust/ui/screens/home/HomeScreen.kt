package com.neetrust.ui.screens.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.neetrust.domain.model.HomeBannerDto
import com.neetrust.domain.model.HomeDataDto
import com.neetrust.domain.model.LatestEpisodeDto
import com.neetrust.ui.components.AnimeCard
import com.neetrust.ui.components.ErrorState
import com.neetrust.ui.components.ExploreChips
import com.neetrust.ui.components.LoadingState
import com.neetrust.ui.components.NeetRustTopBar
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.screens.discover.DiscoverTab
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
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

data class HomeUiState(
    val isLoading: Boolean = true,
    val data: HomeDataDto? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val logger = AppLogger.of("HomeViewModel")

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            logger.i("Loading home screen anime data")
            val result = animeRepository.getHomeData()
            result.fold(
                onSuccess = { data ->
                    logger.i("Home data loaded successfully (${data.latestEpisodes.size} latest, ${data.popularAnime.size} popular)")
                    _uiState.value = _uiState.value.copy(isLoading = false, data = data)
                },
                onFailure = { error ->
                    logger.e("Failed to load home data", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Tidak dapat memuat beranda: ${error.message}"
                    )
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (slug: String) -> Unit,
    onEpisodeClick: (episodeSlug: String) -> Unit,
    onSearchClick: () -> Unit,
    onDiscoverClick: (DiscoverTab) -> Unit = {},
    onSourcesClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoriBgDeep)
            .testTag("home_screen")
    ) {
        when {
            state.isLoading -> {
                LoadingState(
                    message = "Memuat anime terbaru...",
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                )
            }
            state.errorMessage != null && state.data == null -> {
                ErrorState(
                    message = state.errorMessage!!,
                    onRetry = { viewModel.loadHomeData() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 56.dp)
                )
            }
            state.data != null -> {
                val data = state.data!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (data.banners.isNotEmpty()) {
                        item {
                            HomeHeroFull(
                                banner = data.banners.first(),
                                onWatchClick = { onAnimeClick(data.banners.first().slug) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item {
                        ExploreChips(
                            onDiscoverClick = onDiscoverClick,
                            onSourcesClick = onSourcesClick,
                            modifier = if (data.banners.isEmpty()) Modifier.statusBarsPadding() else Modifier
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Episode Terbaru",
                            subtitle = "Update rilis hari ini"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(data.latestEpisodes) { ep ->
                                LatestEpisodeCard(
                                    item = ep,
                                    onClick = { onEpisodeClick(ep.episodeSlug) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Anime Populer",
                            subtitle = "Paling banyak ditonton minggu ini"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(data.popularAnime) { anime ->
                                AnimeCard(
                                    title = anime.title,
                                    posterUrl = anime.poster,
                                    score = anime.score ?: "N/A",
                                    type = anime.type,
                                    status = anime.status,
                                    latestEpisode = anime.latestEpisode,
                                    onClick = { onAnimeClick(anime.slug) }
                                )
                            }
                        }
                    }
                }
            }
        }

        HomeTopOverlay(
            title = AppConfig.APP_NAME,
            onSearchClick = onSearchClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun HomeTopOverlay(
    title: String,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MoriBgDeep.copy(alpha = 0.75f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MoriTextPrimary,
            modifier = Modifier
                .weight(1f)
                .testTag("topbar_title")
        )
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MoriBgDeep.copy(alpha = 0.45f))
                .testTag("home_search_action")
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari Anime",
                tint = MoriTextPrimary
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MoriEmeraldPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = MoriTextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MoriTextMuted
                )
            }
        }
    }
}

@Composable
fun HomeHeroFull(
    banner: HomeBannerDto,
    onWatchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(MoriBgCard)
            .clickable(onClick = onWatchClick)
    ) {
        AsyncImage(
            model = banner.poster,
            contentDescription = banner.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MoriBgDeep.copy(alpha = 0.5f),
                        0.3f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to MoriBgDeep
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            if (banner.genres.isNotEmpty()) {
                Text(
                    text = banner.genres.take(3).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MoriEmeraldLight
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = banner.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
                color = MoriTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!banner.synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = banner.synopsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MoriTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onWatchClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoriEmeraldPrimary,
                    contentColor = MoriTextInverse
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tonton Sekarang", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun LatestEpisodeCard(
    item: LatestEpisodeDto,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("latest_ep_${item.episodeNumber}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(12.dp))
                .background(MoriBgCard)
                .border(1.dp, MoriBorder, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MoriBgDeep.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Putar",
                    tint = MoriEmeraldLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MoriEmeraldDark.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EP ${item.episodeNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = MoriEmeraldLight
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = MoriTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = item.releaseTime,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MoriTextMuted
        )
    }
}
