package com.neetrust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriBorderFocused
import com.neetrust.ui.theme.MoriEmeraldDark
import com.neetrust.ui.theme.MoriEmeraldLight
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary

@Composable
fun EpisodeItem(
    title: String,
    episodeNumber: String,
    uploadedAt: String? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isPlaying) MoriBorderFocused else MoriBorder
    val bgCard = if (isPlaying) MoriEmeraldDark.copy(alpha = 0.35f) else MoriBgCard

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .background(bgCard)
            .border(AppConfig.BORDER_WIDTH, borderColor, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("episode_item_$episodeNumber"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isPlaying) MoriEmeraldPrimary else MoriEmeraldDark.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Putar Episode",
                tint = if (isPlaying) MoriBgCard else MoriEmeraldLight,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isPlaying) MoriEmeraldLight else MoriTextPrimary
            )
            if (!uploadedAt.isNullOrBlank()) {
                Text(
                    text = "Rilis: $uploadedAt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MoriTextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MoriBgCard)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "EP $episodeNumber",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = if (isPlaying) MoriEmeraldLight else MoriTextMuted
            )
        }
    }
}
