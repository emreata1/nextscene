package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SeriesDetailScreen(
    viewModel: SeriesDetailViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val movieDetail by viewModel.movieDetail.collectAsState()
    val currentUser = authViewModel.getCurrentUser()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        movieDetail?.let { detail ->

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = rememberAsyncImagePainter(detail.Poster),
                    contentDescription = "Movie Poster",
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = detail.Title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = detail.Year, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = detail.Runtime, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(text = detail.Genre, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = detail.Plot, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "IMDb Rating: ", style = MaterialTheme.typography.bodyLarge)
                StarRating(imdbRating = detail.imdbRating)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = detail.imdbRating, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
            Text(text = "Rated: ${detail.Rated}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Director: ${detail.Director}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Actors: ${detail.Actors}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(24.dp))

            // -----------------------------------
            // Favorilere Ekle & İzlendi Butonları
            // -----------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            CoroutineScope(Dispatchers.IO).launch {
                                authViewModel.addFavoriteSeries(uid, detail.imdbID)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favorilere Ekle", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        currentUser?.uid?.let { uid ->
                            CoroutineScope(Dispatchers.IO).launch {
                                authViewModel.addWatchedSeries(uid, detail.imdbID)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("İzlendi Olarak İşaretledizi", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

        } ?: run {
            Text(text = "Loading details...", style = MaterialTheme.typography.headlineMedium)
        }
    }
}


