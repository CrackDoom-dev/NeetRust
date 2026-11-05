// [KATALOG] shared.ui.screens.settings
package com.example.neetrust.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neetrust.auth.AuthRepository
import com.example.neetrust.core.logger.AppLogger
import com.example.neetrust.ui.components.NeetRustTopBar
import com.example.neetrust.ui.config.AppConfig
import com.example.neetrust.ui.theme.MoriBgCard
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriBorder
import com.example.neetrust.ui.theme.MoriCoralAlert
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

class SettingsViewModel(
    private val authRepository: AuthRepository,
    context: Context
) : ViewModel() {
    private val logger = AppLogger.of("SettingsViewModel")
    private val prefs = context.getSharedPreferences("neetrust_settings", Context.MODE_PRIVATE)

    private val _defaultQuality = MutableStateFlow(prefs.getString("def_quality", AppConfig.DEFAULT_STREAM_QUALITY) ?: "720p")
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    fun setDefaultQuality(quality: String) {
        logger.i("Setting default stream quality to: $quality")
        prefs.edit().putString("def_quality", quality).apply()
        _defaultQuality.value = quality
    }

    fun clearCache(context: Context, onCleared: () -> Unit) {
        viewModelScope.launch {
            try {
                logger.i("Clearing application disk cache")
                context.cacheDir.deleteRecursively()
                onCleared()
            } catch (e: Exception) {
                logger.e("Failed to clear cache", e)
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showQualityDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Pengaturan",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("settings_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Preferensi Pemutar & Tampilan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MoriEmeraldLight)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                    .background(MoriBgCard)
                    .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            ) {
                // Kualitas Default
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showQualityDropdown = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MoriEmeraldDark.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = MoriEmeraldLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Kualitas Video Default", style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = MoriTextPrimary)
                        Text(text = "Resolusi awal saat memutar video", style = MaterialTheme.typography.labelSmall, color = MoriTextMuted)
                    }
                    Box {
                        Text(text = defaultQuality, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MoriEmeraldPrimary)
                        DropdownMenu(
                            expanded = showQualityDropdown,
                            onDismissRequest = { showQualityDropdown = false },
                            modifier = Modifier.background(MoriBgCard).border(1.dp, MoriBorder, RoundedCornerShape(8.dp))
                        ) {
                            AppConfig.AVAILABLE_QUALITIES.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, color = if (q == defaultQuality) MoriEmeraldLight else MoriTextPrimary) },
                                    onClick = {
                                        viewModel.setDefaultQuality(q)
                                        showQualityDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MoriBorder))

                // Tema
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Tema Aplikasi",
                    subtitle = "Mori (Deep Forest & Twilight Lo-Fi)",
                    trailingText = "Aktif"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Penyimpanan & Jaringan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MoriEmeraldLight)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                    .background(MoriBgCard)
                    .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            ) {
                // Bersihkan Cache
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.clearCache(context) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cache aplikasi berhasil dibersihkan")
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MoriEmeraldDark.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MoriEmeraldLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Bersihkan Cache", style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = MoriTextPrimary)
                        Text(text = "Hapus file sementara & poster tersimpan", style = MaterialTheme.typography.labelSmall, color = MoriTextMuted)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MoriBorder))

                // Backend Host
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "Backend Endpoint",
                    subtitle = AppConfig.BACKEND_BASE_URL,
                    trailingText = "Terhubung"
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MoriBorder))

                // Info App
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Tentang Aplikasi",
                    subtitle = "${AppConfig.APP_NAME} ${AppConfig.APP_VERSION_LABEL}",
                    trailingText = ""
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout
            Button(
                onClick = {
                    viewModel.logout()
                    onLogoutSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_logout_button"),
                shape = RoundedCornerShape(AppConfig.BUTTON_CORNER_RADIUS),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoriCoralAlert.copy(alpha = 0.15f),
                    contentColor = MoriCoralAlert
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingText: String = "",
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MoriEmeraldDark.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MoriEmeraldLight, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = MoriTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MoriTextMuted)
        }
        if (trailingText.isNotEmpty()) {
            Text(text = trailingText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MoriEmeraldPrimary)
        }
    }
}
