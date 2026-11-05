package com.neetrust

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.neetrust.auth.AuthRepository
import com.neetrust.auth.GoogleSignInHandler
import com.neetrust.auth.SessionStore
import com.neetrust.core.network.NetworkClient
import com.neetrust.data.api.ApiService
import com.neetrust.data.repository.AnimeRepository
import com.neetrust.player.PlayerController
import com.neetrust.ui.components.NeetRustBottomNav
import com.neetrust.ui.components.NeetRustTab
import com.neetrust.ui.screens.detail.DetailScreen
import com.neetrust.ui.screens.detail.DetailViewModel
import com.neetrust.ui.screens.favorites.FavoritesScreen
import com.neetrust.ui.screens.favorites.FavoritesViewModel
import com.neetrust.ui.screens.history.HistoryScreen
import com.neetrust.ui.screens.history.HistoryViewModel
import com.neetrust.ui.screens.home.HomeScreen
import com.neetrust.ui.screens.home.HomeViewModel
import com.neetrust.ui.screens.login.LoginScreen
import com.neetrust.ui.screens.login.LoginViewModel
import com.neetrust.ui.screens.player.PlayerScreen
import com.neetrust.ui.screens.player.PlayerViewModel
import com.neetrust.ui.screens.profile.ProfileScreen
import com.neetrust.ui.screens.profile.ProfileViewModel
import com.neetrust.ui.screens.search.SearchScreen
import com.neetrust.ui.screens.search.SearchViewModel
import com.neetrust.ui.screens.settings.SettingsScreen
import com.neetrust.ui.screens.settings.SettingsViewModel
import com.neetrust.ui.screens.splash.SplashScreen
import com.neetrust.ui.screens.splash.SplashViewModel
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.NeetRustTheme

sealed class AppScreen {
    object Splash : AppScreen()
    object Login : AppScreen()
    object Main : AppScreen()
    data class Detail(val animeSlug: String) : AppScreen()
    data class Player(val episodeSlug: String) : AppScreen()
    object Settings : AppScreen()
}

class MainActivity : ComponentActivity() {
    private lateinit var sessionStore: SessionStore
    private lateinit var apiService: ApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var animeRepository: AnimeRepository
    private lateinit var playerController: PlayerController
    private lateinit var googleSignInHandler: GoogleSignInHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionStore = SessionStore(applicationContext)
        apiService = NetworkClient.createService<ApiService>()
        authRepository = AuthRepository(apiService, sessionStore)
        animeRepository = AnimeRepository(applicationContext, apiService, sessionStore)
        playerController = PlayerController(applicationContext)
        googleSignInHandler = GoogleSignInHandler(this)

        setContent {
            NeetRustTheme {
                NeetRustApp(
                    sessionStore = sessionStore,
                    authRepository = authRepository,
                    animeRepository = animeRepository,
                    playerController = playerController,
                    googleSignInHandler = googleSignInHandler
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController.release()
    }
}

@Composable
fun NeetRustApp(
    sessionStore: SessionStore,
    authRepository: AuthRepository,
    animeRepository: AnimeRepository,
    playerController: PlayerController,
    googleSignInHandler: GoogleSignInHandler
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    var currentTab by remember { mutableStateOf(NeetRustTab.HOME) }
    val screenBackStack = remember { mutableListOf<AppScreen>() }

    val homeVm = remember { lazy { HomeViewModel(animeRepository) } }
    val searchVm = remember { lazy { SearchViewModel(animeRepository) } }
    val favVm = remember { lazy { FavoritesViewModel(animeRepository) } }
    val histVm = remember { lazy { HistoryViewModel(animeRepository) } }
    val profVm = remember { lazy { ProfileViewModel(authRepository) } }

    fun navigateTo(destination: AppScreen) {
        screenBackStack.add(currentScreen)
        currentScreen = destination
    }

    fun navigateBack() {
        if (screenBackStack.isNotEmpty()) {
            currentScreen = screenBackStack.removeAt(screenBackStack.size - 1)
        } else {
            currentScreen = AppScreen.Main
        }
    }

    when (val screen = currentScreen) {
        is AppScreen.Splash -> {
            val splashVm = remember { SplashViewModel(sessionStore) }
            SplashScreen(
                viewModel = splashVm,
                onNavigateToHome = { currentScreen = AppScreen.Main },
                onNavigateToLogin = { currentScreen = AppScreen.Login }
            )
        }

        is AppScreen.Login -> {
            val loginVm = remember { LoginViewModel(authRepository, googleSignInHandler) }
            LoginScreen(
                viewModel = loginVm,
                onLoginSuccess = { currentScreen = AppScreen.Main }
            )
        }

        is AppScreen.Main -> {
            Scaffold(
                bottomBar = {
                    NeetRustBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                },
                containerColor = MoriBgDeep,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentTab) {
                        NeetRustTab.HOME -> {
                            HomeScreen(
                                viewModel = homeVm.value,
                                onAnimeClick = { slug -> navigateTo(AppScreen.Detail(slug)) },
                                onEpisodeClick = { epSlug -> navigateTo(AppScreen.Player(epSlug)) },
                                onSearchClick = { currentTab = NeetRustTab.SEARCH }
                            )
                        }

                        NeetRustTab.SEARCH -> {
                            SearchScreen(
                                viewModel = searchVm.value,
                                onAnimeClick = { slug -> navigateTo(AppScreen.Detail(slug)) }
                            )
                        }

                        NeetRustTab.FAVORITES -> {
                            FavoritesScreen(
                                viewModel = favVm.value,
                                onAnimeClick = { slug -> navigateTo(AppScreen.Detail(slug)) },
                                onBrowseClick = { currentTab = NeetRustTab.HOME }
                            )
                        }

                        NeetRustTab.HISTORY -> {
                            HistoryScreen(
                                viewModel = histVm.value,
                                onEpisodeClick = { epSlug -> navigateTo(AppScreen.Player(epSlug)) },
                                onBrowseClick = { currentTab = NeetRustTab.HOME }
                            )
                        }

                        NeetRustTab.PROFILE -> {
                            ProfileScreen(
                                viewModel = profVm.value,
                                onNavigateToSettings = { navigateTo(AppScreen.Settings) },
                                onNavigateToFavorites = { currentTab = NeetRustTab.FAVORITES },
                                onNavigateToHistory = { currentTab = NeetRustTab.HISTORY },
                                onLogoutSuccess = {
                                    screenBackStack.clear()
                                    currentScreen = AppScreen.Login
                                }
                            )
                        }
                    }
                }
            }
        }

        is AppScreen.Detail -> {
            val detailVm = remember(screen.animeSlug) {
                DetailViewModel(screen.animeSlug, animeRepository)
            }
            BackHandler { navigateBack() }
            DetailScreen(
                viewModel = detailVm,
                onNavigateBack = { navigateBack() },
                onEpisodeClick = { epSlug -> navigateTo(AppScreen.Player(epSlug)) }
            )
        }

        is AppScreen.Player -> {
            val playerVm = remember(screen.episodeSlug) {
                PlayerViewModel(screen.episodeSlug, animeRepository, playerController)
            }
            BackHandler { navigateBack() }
            PlayerScreen(
                viewModel = playerVm,
                onNavigateBack = { navigateBack() },
                onNavigateToEpisode = { nextEpSlug ->
                    currentScreen = AppScreen.Player(nextEpSlug)
                }
            )
        }

        is AppScreen.Settings -> {
            val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
            val settingsVm = remember {
                SettingsViewModel(authRepository, context)
            }
            BackHandler { navigateBack() }
            SettingsScreen(
                viewModel = settingsVm,
                onNavigateBack = { navigateBack() },
                onLogoutSuccess = {
                    screenBackStack.clear()
                    currentScreen = AppScreen.Login
                }
            )
        }
    }
}
