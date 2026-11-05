package com.neetrust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriEmeraldDark
import com.neetrust.ui.theme.MoriEmeraldLight
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextMuted

enum class NeetRustTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Beranda", Icons.Default.Home, "home"),
    SEARCH("Cari", Icons.Default.Search, "search"),
    FAVORITES("Favorit", Icons.Default.Bookmark, "favorites"),
    HISTORY("Riwayat", Icons.Default.History, "history"),
    PROFILE("Profil", Icons.Default.Person, "profile")
}

@Composable
fun NeetRustBottomNav(
    currentTab: NeetRustTab,
    onTabSelected: (NeetRustTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .border(width = 1.dp, color = MoriBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MoriBgDeep),
        containerColor = MoriBgDeep,
        tonalElevation = 8.dp
    ) {
        NeetRustTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MoriEmeraldLight,
                    selectedTextColor = MoriEmeraldLight,
                    indicatorColor = MoriEmeraldDark.copy(alpha = 0.5f),
                    unselectedIconColor = MoriTextMuted,
                    unselectedTextColor = MoriTextMuted
                ),
                modifier = Modifier.testTag("nav_item_${tab.route}")
            )
        }
    }
}
