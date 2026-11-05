package com.neetrust.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.neetrust.core.logger.AppLogger
import com.neetrust.data.repository.DiscoverRepository
import com.neetrust.domain.model.AnimeDto
import com.neetrust.domain.model.ScheduleResponseDto
import com.neetrust.ui.components.AnimeCard
import com.neetrust.ui.components.ErrorState
import com.neetrust.ui.components.LoadingState
import com.neetrust.ui.components.NeetRustTopBar
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriEmeraldLight
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
import com.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DiscoverTab(val label: String) {
    SCHEDULE("Jadwal"),
    ONGOING("Ongoing"),
    MOVIES("Movie")
}

data class TabState<T>(
    val isLoading: Boolean = true,
    val data: T? = null,
    val error: String? = null
)

class DiscoverViewModel(
    private val repository: DiscoverRepository
) : ViewModel() {
    private val logger = AppLogger.of("DiscoverViewModel")

    private val _schedule = MutableStateFlow(TabState<ScheduleResponseDto>())
    val schedule: StateFlow<TabState<ScheduleResponseDto>> = _schedule.asStateFlow()

    private val _ongoing = MutableStateFlow(TabState<List<AnimeDto>>())
    val ongoing: StateFlow<TabState<List<AnimeDto>>> = _ongoing.asStateFlow()

    private val _movies = MutableStateFlow(TabState<List<AnimeDto>>())
    val movies: StateFlow<TabState<List<AnimeDto>>> = _movies.asStateFlow()

    fun load(tab: DiscoverTab, force: Boolean = false) {
        when (tab) {
            DiscoverTab.SCHEDULE -> {
                if (!force && _schedule.value.data != null) return
                viewModelScope.launch {
                    _schedule.value = TabState(isLoading = true)
                    repository.schedule().fold(
                        onSuccess = { _schedule.value = TabState(isLoading = false, data = it) },
                        onFailure = {
                            logger.e("Gagal memuat jadwal", it)
                            _schedule.value = TabState(isLoading = false, error = it.message ?: "Gagal memuat jadwal")
                        }
                    )
                }
            }
            DiscoverTab.ONGOING -> {
                if (!force && _ongoing.value.data != null) return
                viewModelScope.launch {
                    _ongoing.value = TabState(isLoading = true)
                    repository.ongoing().fold(
                        onSuccess = { _ongoing.value = TabState(isLoading = false, data = it.items) },
                        onFailure = {
                            logger.e("Gagal memuat ongoing", it)
                            _ongoing.value = TabState(isLoading = false, error = it.message ?: "Gagal memuat daftar ongoing")
                        }
                    )
                }
            }
            DiscoverTab.MOVIES -> {
                if (!force && _movies.value.data != null) return
                viewModelScope.launch {
                    _movies.value = TabState(isLoading = true)
                    repository.movies().fold(
                        onSuccess = { _movies.value = TabState(isLoading = false, data = it.items) },
                        onFailure = {
                            logger.e("Gagal memuat movie", it)
                            _movies.value = TabState(isLoading = false, error = it.message ?: "Gagal memuat daftar movie")
                        }
                    )
                }
            }
        }
    }
}

private fun todayName(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> "Senin"
    Calendar.TUESDAY -> "Selasa"
    Calendar.WEDNESDAY -> "Rabu"
    Calendar.THURSDAY -> "Kamis"
    Calendar.FRIDAY -> "Jumat"
    Calendar.SATURDAY -> "Sabtu"
    else -> "Minggu"
}

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    initialTab: DiscoverTab,
    onNavigateBack: () -> Unit,
    onAnimeClick: (slug: String) -> Unit
) {
    var tab by remember { mutableStateOf(initialTab) }
    val schedule by viewModel.schedule.collectAsState()
    val ongoing by viewModel.ongoing.collectAsState()
    val movies by viewModel.movies.collectAsState()

    LaunchedEffect(tab) { viewModel.load(tab) }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Jelajah",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("discover_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = MoriBgDeep,
                contentColor = MoriEmeraldPrimary
            ) {
                DiscoverTab.values().forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = {
                            Text(
                                text = t.label,
                                color = if (tab == t) MoriEmeraldLight else MoriTextSecondary
                            )
                        }
                    )
                }
            }

            when (tab) {
                DiscoverTab.SCHEDULE -> ScheduleContent(
                    state = schedule,
                    onRetry = { viewModel.load(DiscoverTab.SCHEDULE, force = true) },
                    onAnimeClick = onAnimeClick
                )
                DiscoverTab.ONGOING -> AnimeGridContent(
                    state = ongoing,
                    emptyText = "Belum ada anime ongoing dari sumber ini.",
                    onRetry = { viewModel.load(DiscoverTab.ONGOING, force = true) },
                    onAnimeClick = onAnimeClick
                )
                DiscoverTab.MOVIES -> AnimeGridContent(
                    state = movies,
                    emptyText = "Belum ada movie dari sumber ini.",
                    onRetry = { viewModel.load(DiscoverTab.MOVIES, force = true) },
                    onAnimeClick = onAnimeClick
                )
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    state: TabState<ScheduleResponseDto>,
    onRetry: () -> Unit,
    onAnimeClick: (String) -> Unit
) {
    val data = state.data
    when {
        state.isLoading -> LoadingState(message = "Memuat jadwal tayang...")
        data == null -> ErrorState(message = state.error ?: "Jadwal tidak tersedia", onRetry = onRetry)
        data.days.isEmpty() -> ErrorState(
            message = "Jadwal belum bisa dibaca dari sumber ini.",
            onRetry = onRetry
        )
        else -> {
            val today = todayName()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.days.forEach { day ->
                    item {
                        Text(
                            text = if (day.day == today) "${day.day} • Hari ini" else day.day,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (day.day == today) MoriEmeraldLight else MoriTextPrimary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(day.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MoriBgCard)
                                .clickable { onAnimeClick(item.slug) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.poster.isNotBlank()) {
                                AsyncImage(
                                    model = item.poster,
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MoriTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!item.time.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tayang ${item.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MoriTextMuted
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

@Composable
private fun AnimeGridContent(
    state: TabState<List<AnimeDto>>,
    emptyText: String,
    onRetry: () -> Unit,
    onAnimeClick: (String) -> Unit
) {
    val data = state.data
    when {
        state.isLoading -> LoadingState(message = "Memuat daftar...")
        data == null -> ErrorState(message = state.error ?: "Data tidak tersedia", onRetry = onRetry)
        data.isEmpty() -> ErrorState(message = emptyText, onRetry = onRetry)
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 135.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(data) { anime ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
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
