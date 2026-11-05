// [KATALOG] shared.ui.screens.splash
package com.example.neetrust.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neetrust.auth.SessionStore
import com.example.neetrust.core.logger.AppLogger
import com.example.neetrust.ui.config.AppConfig
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.MoriEmeraldDark
import com.example.neetrust.ui.theme.MoriEmeraldPrimary
import com.example.neetrust.ui.theme.MoriTextMuted
import com.example.neetrust.ui.theme.MoriTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val sessionStore: SessionStore
) : ViewModel() {
    private val logger = AppLogger.of("SplashViewModel")

    sealed class SplashEvent {
        object NavigateToHome : SplashEvent()
        object NavigateToLogin : SplashEvent()
    }

    private val _event = MutableSharedFlow<SplashEvent>()
    val event: SharedFlow<SplashEvent> = _event.asSharedFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            logger.i("Starting splash timeout")
            delay(AppConfig.SPLASH_DURATION_MS)
            val hasUser = sessionStore.currentUser.value != null || sessionStore.isGuestMode.value
            if (hasUser) {
                logger.i("Existing session found, navigating to Home")
                _event.emit(SplashEvent.NavigateToHome)
            } else {
                logger.i("No active session, navigating to Login")
                _event.emit(SplashEvent.NavigateToLogin)
            }
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1.0f, animationSpec = tween(700))
        }
        launch {
            alpha.animateTo(1.0f, animationSpec = tween(700))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                SplashViewModel.SplashEvent.NavigateToHome -> onNavigateToHome()
                SplashViewModel.SplashEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoriBgDeep)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MoriEmeraldDark.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Logo NeetRust",
                    tint = MoriEmeraldPrimary,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = AppConfig.SPLASH_TITLE,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MoriTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = AppConfig.SPLASH_SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                color = MoriTextMuted
            )
        }

        Text(
            text = AppConfig.APP_VERSION_LABEL,
            style = MaterialTheme.typography.labelSmall,
            color = MoriTextMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
