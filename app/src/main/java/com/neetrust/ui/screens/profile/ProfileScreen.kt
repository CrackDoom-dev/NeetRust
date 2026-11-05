package com.neetrust.ui.screens.profile

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.neetrust.auth.AuthRepository
import com.neetrust.core.logger.AppLogger
import com.neetrust.domain.model.UserProfileDto
import com.neetrust.ui.components.NeetRustTopBar
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriBgSurface
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriCoralAlert
import com.neetrust.ui.theme.MoriEmeraldDark
import com.neetrust.ui.theme.MoriEmeraldLight
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextInverse
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
import com.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val logger = AppLogger.of("ProfileViewModel")
    val currentUser: StateFlow<UserProfileDto?> = authRepository.currentUser
    val isGuestMode: StateFlow<Boolean> = authRepository.isGuestMode

    fun updateProfile(name: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            logger.i("Updating profile: $name")
            authRepository.updateProfile(name = name, bio = bio, avatarUrl = avatarUrl)
        }
    }

    fun logout() {
        logger.i("Logging out user from profile")
        authRepository.logout()
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val isGuest by viewModel.isGuestMode.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Profil Pengguna",
                canNavigateBack = false,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = MoriTextPrimary)
                    }
                }
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("profile_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                    .background(MoriBgCard)
                    .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MoriEmeraldDark)
                            .border(2.dp, MoriEmeraldPrimary, CircleShape)
                    ) {
                        val avatar = user?.avatarUrl?.ifBlank { AppConfig.DEFAULT_AVATAR_URL } ?: AppConfig.DEFAULT_AVATAR_URL
                        AsyncImage(
                            model = avatar,
                            contentDescription = "Avatar Pengguna",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.name ?: AppConfig.GUEST_USER_NAME,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MoriTextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = user?.email ?: "guest@neetrust.app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoriTextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGuest) MoriBgSurface else MoriEmeraldDark.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGuest) Icons.Default.Person else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isGuest) MoriTextMuted else MoriEmeraldLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGuest) "Mode Tamu (Lokal)" else "Akun Google Terhubung",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isGuest) MoriTextSecondary else MoriEmeraldLight
                            )
                        }
                    }

                    if (!user?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = user!!.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoriTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MoriEmeraldLight),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MoriBorder)
                        ),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profil")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                    .background(MoriBgCard)
                    .border(1.dp, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.Bookmark,
                    title = "Daftar Favorit Saya",
                    subtitle = "Anime yang Anda tandai",
                    onClick = onNavigateToFavorites
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MoriBorder))
                ProfileMenuItem(
                    icon = Icons.Default.History,
                    title = "Riwayat Menonton",
                    subtitle = "Lanjutkan episode terakhir",
                    onClick = onNavigateToHistory
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MoriBorder))
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Pengaturan Aplikasi",
                    subtitle = "Kualitas stream, tema & cache",
                    onClick = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.logout()
                    onLogoutSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
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

        if (showEditDialog) {
            EditProfileDialog(
                currentName = user?.name ?: "",
                currentBio = user?.bio ?: "",
                currentAvatar = user?.avatarUrl ?: "",
                onDismiss = { showEditDialog = false },
                onSave = { name, bio, avatar ->
                    viewModel.updateProfile(name, bio, avatar)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = MoriTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MoriTextMuted)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MoriTextMuted)
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, avatar: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var avatar by remember { mutableStateOf(currentAvatar) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Profil", style = MaterialTheme.typography.titleLarge, color = MoriTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Tampilan") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoriEmeraldPrimary,
                        unfocusedBorderColor = MoriBorder,
                        focusedTextColor = MoriTextPrimary,
                        unfocusedTextColor = MoriTextPrimary
                    )
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Status Singkat") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoriEmeraldPrimary,
                        unfocusedBorderColor = MoriBorder,
                        focusedTextColor = MoriTextPrimary,
                        unfocusedTextColor = MoriTextPrimary
                    )
                )
                OutlinedTextField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = { Text("URL Foto Profil") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoriEmeraldPrimary,
                        unfocusedBorderColor = MoriBorder,
                        focusedTextColor = MoriTextPrimary,
                        unfocusedTextColor = MoriTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, bio, avatar) },
                colors = ButtonDefaults.buttonColors(containerColor = MoriEmeraldPrimary, contentColor = MoriTextInverse)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = MoriTextSecondary)
            }
        },
        containerColor = MoriBgCard
    )
}
