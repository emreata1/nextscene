package com.example.nextscene.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument


@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    if (screen.icon != null) {
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Timeline.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Series.route) { SeriesScreen(navController = navController) }
            composable(Screen.Films.route) { MoviesScreen(navController = navController) }
            composable(Screen.Favorites.route) { FavoritesScreen() }
            composable(Screen.Auth.route) {
                val currentUser = authViewModel.getCurrentUser()

                if (currentUser == null) {
                    AuthScreen(
                        authViewModel = authViewModel,
                        onAuthSuccess = {
                            navController.navigate(Screen.Series.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                } else {
                    OpenProfileScreen(navController = navController)
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            composable(
                route = Screen.DetailMovie.route,
                arguments = Screen.DetailMovie.navArguments
            ) { backStackEntry ->
                val imdbID = backStackEntry.arguments?.getString("imdbID")
                if (imdbID != null) {
                    MovieDetailScreen()
                }
            }
            composable(
                route = Screen.DetailSeries.route,
                arguments = Screen.DetailSeries.navArguments
            ) { backStackEntry ->
                val imdbID = backStackEntry.arguments?.getString("imdbID")
                if (imdbID != null) {
                    SeriesDetailScreen()
                }
            }

            composable("openProfile/{uid}") { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid")
                if (uid != null) {
                    // BURASI ÖNEMLİ: targetUid parametresini gönderiyoruz
                    OpenProfileScreen(
                        navController = navController,
                        targetUid = uid
                    )
                }
            }

            // Ayrıca kendi profilin için olan kısmı da kontrol et:
            composable(Screen.Auth.route) {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    AuthScreen(
                        authViewModel = authViewModel,
                        onAuthSuccess = {
                            navController.navigate(Screen.Series.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    )
                } else {
                    // Kendi profilin olduğu için targetUid null veya kendi uid olabilir
                    OpenProfileScreen(navController = navController, targetUid = currentUser.uid)
                }
            }

            composable(Screen.Timeline.route) {
                TimelineScreen(navController = navController)
            }

            composable(
                route = Screen.FollowList.route,
                arguments = Screen.FollowList.navArguments
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid")
                val type = backStackEntry.arguments?.getString("type")
                if (uid != null && type != null) {
                    FollowListScreen(
                        navController = navController,
                        targetUid = uid,
                        listType = type
                    )
                }
            }

            // Diğer sayfaların (composable'ların) olduğu yere ekle:

            composable(
                route = "full_list_screen/{userId}/{listType}/{pageTitle}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("listType") { type = NavType.StringType },
                    navArgument("pageTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                // Parametreleri alıyoruz
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val listType = backStackEntry.arguments?.getString("listType") ?: ""
                val pageTitle = backStackEntry.arguments?.getString("pageTitle") ?: ""

                // Ekranı çağırıyoruz
                FullListScreen(
                    navController = navController,
                    userId = userId,
                    listType = listType,
                    pageTitle = pageTitle
                )
            }

        }
    }
}

