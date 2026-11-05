package com.neetrust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.neetrust.ui.screens.discover.DiscoverTab
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriTextPrimary

@Composable
fun ExploreChips(
    onDiscoverClick: (DiscoverTab) -> Unit,
    onSourcesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiscoverTab.values().toList()) { tab ->
            ExploreChip(label = tab.label, onClick = { onDiscoverClick(tab) })
        }
        item {
            ExploreChip(label = "Sumber", onClick = onSourcesClick)
        }
    }
}

@Composable
private fun ExploreChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(MoriBgCard)
            .border(1.dp, MoriBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MoriTextPrimary
        )
    }
}
