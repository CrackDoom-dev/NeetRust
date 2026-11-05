// [KATALOG] shared.ui.screens.search
package com.example.neetrust.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neetrust.core.logger.AppLogger
import com.example.neetrust.data.repository.AnimeRepository
import com.example.neetrust.domain.model.AnimeDto
import com.example.neetrust.ui.components.AnimeCard
import com.example.neetrust.ui.components.NeetRustTopBar
import com.example.neetrust.ui.config.AppConfig
import com.example.neetrust.ui.theme.MoriBgCard
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriBgSurface
import com.example.neetrust.ui.theme.MoriBorder
import com.example.neetrust.ui.theme.MoriEmeraldDark
import com.example.neetrust.ui.theme.MoriEmeraldLight
import com.example.neetrust.ui.theme.MoriEmeraldPrimary
import com.example.neetrust.ui.theme.MoriTextMuted
import com.example.neetrust.ui.theme.MoriTextPrimary
import com.example.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val results: List<AnimeDto> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val logger = AppLogger.of("SearchViewModel")

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val availableGenres = listOf("Semua", "Aksi", "Fantasi", "Isekai", "Petualangan", "Komedi", "Romantis", "Drama", "Sci-Fi")

    init {
        // Initial popular fetch
        fetchCatalog(null)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        if (newQuery.length >= 2) {
            performSearch(newQuery)
        } else if (newQuery.isEmpty()) {
            fetchCatalog(_uiState.value.selectedGenre)
        }
    }

    fun selectGenre(genre: String) {
        val selected = if (genre == "Semua") null else genre
        _uiState.value = _uiState.value.copy(selectedGenre = selected, query = "")
        fetchCatalog(selected)
    }

    fun performSearch(q: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            logger.i("Searching for anime with query: $q")
            val res = animeRepository.searchAnime(q)
            res.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(isLoading = false, results = list)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
                }
            )
        }
    }

    private fun fetchCatalog(genre: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = animeRepository.getAnimeCatalog(page = 1, genre = genre?.lowercase())
            res.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(isLoading = false, results = list)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
                }
            )
        }
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAnimeClick: (slug: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Cari Anime",
                canNavigateBack = false
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("search_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input Box
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input_field"),
                placeholder = {
                    Text("Cari judul anime...", color = MoriTextMuted)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MoriEmeraldPrimary)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Bersihkan", tint = MoriTextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoriEmeraldPrimary,
                    unfocusedBorderColor = MoriBorder,
                    focusedContainerColor = MoriBgCard,
                    unfocusedContainerColor = MoriBgCard,
                    focusedTextColor = MoriTextPrimary,
                    unfocusedTextColor = MoriTextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    viewModel.performSearch(state.query)
                })
            )

            // Genre quick filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.availableGenres) { genre ->
                    val isSelected = (genre == "Semua" && state.selectedGenre == null) || (genre == state.selectedGenre)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MoriEmeraldDark else MoriBgSurface)
                            .border(1.dp, if (isSelected) MoriEmeraldPrimary else MoriBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectGenre(genre) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MoriEmeraldLight else MoriTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Results grid
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MoriEmeraldPrimary)
                    }
                }
                state.results.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MoriTextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak ada anime ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                color = MoriTextSecondary
                            )
                            Text(
                                text = "Coba kata kunci atau genre lain",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoriTextMuted
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.results) { anime ->
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
}
