// [KATALOG] androidApp.MainActivity
package com.example

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
import com.example.neetrust.auth.AuthRepository
import com.example.neetrust.auth.GoogleSignInHandler
import com.example.neetrust.auth.SessionStore
import com.example.neetrust.core.network.NetworkClient
import com.example.neetrust.data.api.ApiService
import com.example.neetrust.data.repository.AnimeRepository
import com.example.neetrust.player.PlayerController
import com.example.neetrust.ui.components.NeetRustBottomNav
import com.example.neetrust.ui.components.NeetRustTab
import com.example.neetrust.ui.screens.detail.DetailScreen
import com.example.neetrust.ui.screens.detail.DetailViewModel
import com.example.neetrust.ui.screens.favorites.FavoritesScreen
import com.example.neetrust.ui.screens.favorites.FavoritesViewModel
import com.example.neetrust.ui.screens.history.HistoryScreen
import com.example.neetrust.ui.screens.history.HistoryViewModel
import com.example.neetrust.ui.screens.home.HomeScreen
import com.example.neetrust.ui.screens.home.HomeViewModel
import com.example.neetrust.ui.screens.login.LoginScreen
import com.example.neetrust.ui.screens.login.LoginViewModel
import com.example.neetrust.ui.screens.player.PlayerScreen
import com.example.neetrust.ui.screens.player.PlayerViewModel
import com.example.neetrust.ui.screens.profile.ProfileScreen
import com.example.neetrust.ui.screens.profile.ProfileViewModel
import com.example.neetrust.ui.screens.search.SearchScreen
import com.example.neetrust.ui.screens.search.SearchViewModel
import com.example.neetrust.ui.screens.settings.SettingsScreen
import com.example.neetrust.ui.screens.settings.SettingsViewModel
import com.example.neetrust.ui.screens.splash.SplashScreen
import com.example.neetrust.ui.screens.splash.SplashViewModel
import com.example.neetrust.ui.theme.MoriBgDeep
import com.example.neetrust.ui.theme.NeetRustTheme

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

        // Initialize core dependencies
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
        // PiP state handling is managed reactively
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
                            val homeVm = remember { HomeViewModel(animeRepository) }
                            HomeScreen(
                                viewModel = homeVm,
                                onAnimeClick = { slug -> navigateTo(AppScreen.Detail(slug)) },
                                onEpisodeClick = { epSlug -> navigateTo(AppScreen.Player(epSlug)) },
                                onSearchClick = { currentTab = NeetRustTab.SEARCH }
                            )
                        }

                        NeetRustTab.SEARCH -> {
                            val searchVm = remember { SearchViewModel(animeRepository) }
                            SearchScreen(
                                viewModel = searchVm,
                                onAnimeClick = { slug -> navigateTo(AppScreen.Detail(slug)) }
                            )
                        }

                        NeetRustTab.FAVORITES -> {
                            val favVm = remember { FavoritesViewModel(animeRepository) }
                            FavoritesScreen(
                                viewModel = favVm,
                                onAnimeClick = { slug -> navigateTo(AppScreen.Detail(slug)) },
                                onBrowseClick = { currentTab = NeetRustTab.HOME }
                            )
                        }

                        NeetRustTab.HISTORY -> {
                            val histVm = remember { HistoryViewModel(animeRepository) }
                            HistoryScreen(
                                viewModel = histVm,
                                onEpisodeClick = { epSlug -> navigateTo(AppScreen.Player(epSlug)) },
                                onBrowseClick = { currentTab = NeetRustTab.HOME }
                            )
                        }

                        NeetRustTab.PROFILE -> {
                            val profVm = remember { ProfileViewModel(authRepository) }
                            ProfileScreen(
                                viewModel = profVm,
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
