package com.neetrust.ui.screens.favorites

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.neetrust.core.logger.AppLogger
import com.neetrust.data.repository.AnimeRepository
import com.neetrust.domain.model.FavoriteItemDto
import com.neetrust.ui.components.NeetRustTopBar
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.theme.MoriAmberAccent
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriCoralAlert
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextInverse
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
import com.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val logger = AppLogger.of("FavoritesViewModel")
    val favorites: StateFlow<List<FavoriteItemDto>> = animeRepository.favorites

    fun loadFavorites() {
        viewModelScope.launch {
            logger.i("Refreshing user favorites")
            animeRepository.refreshFavorites()
        }
    }

    fun removeFavorite(animeSlug: String) {
        viewModelScope.launch {
            logger.i("Removing favorite: $animeSlug")
            animeRepository.removeFavorite(animeSlug)
        }
    }
}

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onAnimeClick: (slug: String) -> Unit,
    onBrowseClick: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Anime Favorit (${favorites.size})",
                canNavigateBack = false
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("favorites_screen")
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MoriTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Belum Ada Favorit",
                        style = MaterialTheme.typography.titleLarge,
                        color = MoriTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simpan anime yang ingin Anda tonton agar mudah ditemukan kembali di sini.",
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
                        Text("Jelajahi Beranda")
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
                items(favorites, key = { it.animeSlug }) { fav ->
                    FavoriteCardItem(
                        item = fav,
                        onClick = { onAnimeClick(fav.animeSlug) },
                        onRemove = { viewModel.removeFavorite(fav.animeSlug) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteCardItem(
    item: FavoriteItemDto,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .background(MoriBgCard)
            .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(65.dp)
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(8.dp))
                .background(MoriBgDeep)
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = MoriTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MoriAmberAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = item.score,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MoriAmberAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MoriTextMuted
                )
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.testTag("remove_fav_${item.animeSlug}")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Hapus dari Favorit",
                tint = MoriCoralAlert,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
