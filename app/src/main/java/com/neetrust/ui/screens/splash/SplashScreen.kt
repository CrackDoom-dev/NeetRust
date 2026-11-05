package com.neetrust.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neetrust.auth.SessionStore
import com.neetrust.core.logger.AppLogger
import com.neetrust.ui.config.AppConfig
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
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

private val RustLight = Color(0xFFFF8A4C)
private val RustDark = Color(0xFFD9480F)

@Composable
private fun NeetRustMark(draw: Float, fill: Float, pop: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 60f
        val n = Path().apply {
            moveTo(8f * u, 54f * u)
            lineTo(8f * u, 6f * u)
            lineTo(18f * u, 6f * u)
            lineTo(42f * u, 35f * u)
            lineTo(42f * u, 6f * u)
            lineTo(52f * u, 6f * u)
            lineTo(52f * u, 54f * u)
            lineTo(42f * u, 54f * u)
            lineTo(18f * u, 25f * u)
            lineTo(18f * u, 54f * u)
            close()
        }
        val measure = PathMeasure()
        measure.setPath(n, true)
        val segment = Path()
        measure.getSegment(0f, measure.length * draw, segment, true)
        drawPath(
            path = segment,
            color = Color(0xFFF8F8FA),
            style = Stroke(width = 3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(path = n, color = Color(0xFFF8F8FA).copy(alpha = fill))

        val tri = Path().apply {
            moveTo(22f * u, 40f * u)
            lineTo(22f * u, 52f * u)
            lineTo(31f * u, 46f * u)
            close()
        }
        scale(scale = pop, pivot = Offset(25f * u, 46f * u)) {
            drawPath(
                path = tri,
                brush = Brush.linearGradient(
                    colors = listOf(RustLight, RustDark),
                    start = Offset(22f * u, 40f * u),
                    end = Offset(31f * u, 52f * u)
                )
            )
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val draw = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            progress.animateTo(
                1f,
                animationSpec = tween(AppConfig.SPLASH_DURATION_MS.toInt(), easing = LinearEasing)
            )
        }
        draw.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        launch { fill.animateTo(1f, animationSpec = tween(350)) }
        launch {
            pop.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        text.animateTo(1f, animationSpec = tween(500))
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
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(RustDark.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.minDimension * 0.95f
                    )
                )
            }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NeetRustMark(
                draw = draw.value,
                fill = fill.value,
                pop = pop.value,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = AppConfig.SPLASH_TITLE.uppercase(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 8.sp
                ),
                color = MoriTextPrimary,
                modifier = Modifier.graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 24f
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = AppConfig.SPLASH_SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                color = MoriTextMuted,
                modifier = Modifier.graphicsLayer { alpha = text.value }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MoriTextMuted.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(RustLight, RustDark)))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = AppConfig.APP_VERSION_LABEL,
                style = MaterialTheme.typography.labelSmall,
                color = MoriTextMuted
            )
        }
    }
}
