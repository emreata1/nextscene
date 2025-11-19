package com.example.nextscene.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.network.NetworkModule
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullListScreen(
    navController: NavController,
    userId: String,
    listType: String,
    pageTitle: String
) {
    val db = FirebaseFirestore.getInstance()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("Hepsi", "Filmler", "Diziler")

    var movies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var series by remember { mutableStateOf(listOf<MovieDetail>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId, listType) {
        isLoading = true
        try {
            val userRef = db.collection("users").document(userId)

            val movieCollection = when(listType) {
                "watched" -> "watchedMovies"
                "favorites" -> "favoriteMovies"
                "watchlist" -> "watchlistMovies"
                else -> "watchedMovies"
            }

            val seriesCollection = when(listType) {
                "watched" -> "watchedSeries"
                "favorites" -> "favoriteSeries"
                "watchlist" -> "watchlistSeries"
                else -> "watchedSeries"
            }

            val mIds = userRef.collection(movieCollection).get().await().documents.mapNotNull { it.getString("movieId") }
            val sIds = userRef.collection(seriesCollection).get().await().documents.mapNotNull { it.getString("seriesId") }

            val apiKey = "b9bd48a6"

            val fetchedMovies = mIds.map { id ->
                async {
                    NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey)
                }
            }.awaitAll().filterNotNull()

            val fetchedSeries = sIds.map { id ->
                async {
                    NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey)
                }
            }.awaitAll().filterNotNull()

            movies = fetchedMovies
            series = fetchedSeries

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val displayList = remember(selectedFilterIndex, movies, series) {
        val combined = when (selectedFilterIndex) {
            1 -> movies
            2 -> series
            else -> (movies + series).shuffled() // Hepsi
        }
        combined
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(pageTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            TabRow(selectedTabIndex = selectedFilterIndex) {
                filters.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilterIndex == index,
                        onClick = { selectedFilterIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bu kategoride içerik yok.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp), // Ekran boyutuna göre sığdırır
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayList) { item ->
                        Column(
                            modifier = Modifier
                                .clickable {
                                    // Detaya Git
                                    val route = if (item.Type == "series") "detailseries/${item.imdbID}" else "detailmovie/${item.imdbID}"
                                    navController.navigate(route)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(item.Poster),
                                contentDescription = item.Title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.67f) // Poster oranı
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            item.Title?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}