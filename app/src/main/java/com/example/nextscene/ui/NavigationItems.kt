package com.example.nextscene.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Info
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null, val navArguments: List<NamedNavArgument> = emptyList()) {
    object Series : Screen("series", "Series", Icons.Default.Home)
    object Films : Screen("films", "Films", Icons.Default.Search)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object Auth : Screen("auth", "Profile", Icons.Default.Person)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Detail : Screen(
        "detail/{imdbID}",
        "Detail",
        Icons.Default.Info,
        listOf(navArgument("imdbID") { type = NavType.StringType })
    )
}

val items = listOf(
    Screen.Series,
    Screen.Films,
    Screen.Favorites,
    Screen.Auth,
    Screen.Settings
)