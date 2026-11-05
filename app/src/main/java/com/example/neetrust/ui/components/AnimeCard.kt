// [KATALOG] shared.ui.components.AnimeCard
package com.example.neetrust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.neetrust.ui.config.AppConfig
import com.example.neetrust.ui.theme.MoriAmberAccent
import com.example.neetrust.ui.theme.MoriBgCard
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriBorder
import com.example.neetrust.ui.theme.MoriEmeraldDark
import com.example.neetrust.ui.theme.MoriEmeraldLight
import com.example.neetrust.ui.theme.MoriTextMuted
import com.example.neetrust.ui.theme.MoriTextPrimary

@Composable
fun AnimeCard(
    title: String,
    posterUrl: String,
    score: String = "N/A",
    type: String? = "TV",
    status: String? = null,
    latestEpisode: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(135.dp)
            .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            .clickable(onClick = onClick)
            .testTag("anime_card_${title.take(15)}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                .background(MoriBgCard)
                .border(AppConfig.BORDER_WIDTH, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Rating Badge (Kanan Atas)
            if (score != "N/A" && score.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MoriBgDeep.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = MoriAmberAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = score,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = MoriAmberAccent
                    )
                }
            }

            // Episode Badge / Type (Kiri Bawah)
            val badgeText = latestEpisode?.let { "Ep $it" } ?: type
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MoriEmeraldDark.copy(alpha = 0.90f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = MoriEmeraldLight
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = MoriTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )

        if (!status.isNullOrBlank()) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MoriTextMuted,
                maxLines = 1
            )
        }
    }
}
