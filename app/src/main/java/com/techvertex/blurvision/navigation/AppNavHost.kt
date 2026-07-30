package com.techvertex.blurvision.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.techvertex.blurvision.feature.home.HomeScreen
import com.techvertex.blurvision.feature.home.HomeViewModel
import com.techvertex.blurvision.feature.intro.IntroScreen
import com.techvertex.blurvision.feature.intro.IntroViewModel
import com.techvertex.blurvision.feature.splash.SplashScreen
import com.techvertex.blurvision.feature.splash.SplashViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Intro : Screen("intro")
    data object Home : Screen("home")
    data object Details : Screen("details")
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
                }
            )
        }
        composable(Screen.Details.route) {
            //
        }
    }
}
