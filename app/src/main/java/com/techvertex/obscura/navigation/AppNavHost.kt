package com.techvertex.obscura.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.techvertex.obscura.feature.blurvideo.BlurVideoScreen
import com.techvertex.obscura.feature.blurvideo.BlurVideoViewModel
import com.techvertex.obscura.feature.home.HomeScreen
import com.techvertex.obscura.feature.home.HomeViewModel
import com.techvertex.obscura.feature.intro.IntroScreen
import com.techvertex.obscura.feature.intro.IntroViewModel
import com.techvertex.obscura.feature.splash.SplashScreen
import com.techvertex.obscura.feature.splash.SplashViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Intro : Screen("intro")
    data object Home : Screen("home")
    data object Details : Screen("details")
    data object BlurVideo : Screen("blur_video/{videoUri}") {
        fun createRoute(videoUri: String) = "blur_video/$videoUri"
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToIntro = {
                    navController.navigate(Screen.Intro.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Intro.route) {
            val introViewModel: IntroViewModel = hiltViewModel()
            IntroScreen(
                viewModel = introViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToDetails = { item ->
                    // Navigate to details screen
                },
                onNavigateToBlurVideo = { videoUri ->
                    navController.navigate(Screen.BlurVideo.createRoute(videoUri))
                }
            )
        }

        composable(
            route = Screen.BlurVideo.route,
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoUriStr = backStackEntry.arguments?.getString("videoUri") ?: ""
            val blurVideoViewModel: BlurVideoViewModel = hiltViewModel()
            BlurVideoScreen(
                videoUriStr = videoUriStr,
                viewModel = blurVideoViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Details.route) {
            // Details screen
        }
    }
}
