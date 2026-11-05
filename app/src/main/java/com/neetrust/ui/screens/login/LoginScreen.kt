package com.neetrust.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neetrust.auth.AuthRepository
import com.neetrust.auth.GoogleSignInHandler
import com.neetrust.core.logger.AppLogger
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriCoralAlert
import com.neetrust.ui.theme.MoriEmeraldDark
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextInverse
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
import com.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInHandler: GoogleSignInHandler
) : ViewModel() {
    private val logger = AppLogger.of("LoginViewModel")

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginSuccessEvent = MutableSharedFlow<Unit>()
    val loginSuccessEvent: SharedFlow<Unit> = _loginSuccessEvent.asSharedFlow()

    fun signInWithGoogle(serverClientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            logger.i("Initiating Google Sign-In with serverClientId")
            val tokenResult = googleSignInHandler.requestGoogleIdToken(serverClientId)
            tokenResult.fold(
                onSuccess = { idToken ->
                    val authResult = authRepository.loginWithGoogle(idToken)
                    authResult.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            _loginSuccessEvent.emit(Unit)
                        },
                        onFailure = { err ->
                            logger.e("Failed to authenticate with backend", err)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Gagal login ke server: ${err.message ?: "Silakan coba lagi"}"
                            )
                        }
                    )
                },
                onFailure = { err ->
                    logger.w("Google sign-in cancelled or failed: ${err.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Login Google dibatalkan atau belum dikonfigurasi. Anda dapat menggunakan Mode Tamu."
                    )
                }
            )
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            logger.i("User selected guest mode")
            authRepository.continueAsGuest()
            _loginSuccessEvent.emit(Unit)
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.loginSuccessEvent.collect {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoriBgDeep)
            .padding(24.dp)
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                .background(MoriBgCard)
                .border(AppConfig.BORDER_WIDTH, MoriBorder, RoundedCornerShape(AppConfig.CARD_CORNER_RADIUS))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MoriEmeraldDark.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Logo",
                    tint = MoriEmeraldPrimary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Masuk ke ${AppConfig.APP_NAME}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MoriTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sinkronkan anime favorit dan riwayat tontonan Anda di berbagai perangkat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MoriTextSecondary,
                textAlign = TextAlign.Center
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MoriCoralAlert.copy(alpha = 0.15f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MoriCoralAlert,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MoriEmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Menghubungkan akun...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoriTextSecondary
                )
            } else {
                Button(
                    onClick = {
                        viewModel.signInWithGoogle(serverClientId = "default_google_client_id")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_login_button"),
                    shape = RoundedCornerShape(AppConfig.BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoriEmeraldPrimary,
                        contentColor = MoriTextInverse
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Masuk dengan Google",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { viewModel.continueAsGuest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("guest_login_button"),
                    shape = RoundedCornerShape(AppConfig.BUTTON_CORNER_RADIUS),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MoriTextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MoriBorder)
                    )
                ) {
                    Text(
                        text = "Lanjutkan sebagai Tamu",
                        fontWeight = FontWeight.Medium,
                        color = MoriTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mode tamu menyimpan riwayat & favorit secara lokal di perangkat Anda.",
                style = MaterialTheme.typography.labelSmall,
                color = MoriTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
