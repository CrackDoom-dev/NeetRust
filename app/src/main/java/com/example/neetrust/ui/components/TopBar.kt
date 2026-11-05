// [KATALOG] shared.ui.components.TopBar
package com.example.neetrust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neetrust.ui.theme.MoriBgCard
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriBorder
import com.example.neetrust.ui.theme.MoriEmeraldPrimary
import com.example.neetrust.ui.theme.MoriTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeetRustTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MoriTextPrimary,
                modifier = Modifier.testTag("topbar_title")
            )
        },
        navigationIcon = {
            if (canNavigateBack && onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("topbar_back_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MoriBgCard)
                            .border(1.dp, MoriBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MoriEmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MoriBgDeep,
            titleContentColor = MoriTextPrimary
        ),
        modifier = modifier
    )
}
