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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color // Added import
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.R
// Removed unused CoroutineScope, Dispatchers, launch imports
import kotlin.math.floor

import androidx.lifecycle.SavedStateHandle // Needed for initializer approach
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun MovieDetailScreen(
    // The authViewModel is now passed to the MovieDetailViewModel via the factory
    authViewModel: AuthViewModel = viewModel()
) {
    // Create the MovieDetailViewModel using a custom factory to inject AuthViewModel
    val viewModel: MovieDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                // Access SavedStateHandle from the initializer scope
                MovieDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    authViewModel = authViewModel
                )
            }
        }
    )

    val movieDetail by viewModel.movieDetail.collectAsState()
    // val currentUser = authViewModel.getCurrentUser() // No longer directly used for toggling, kept for other potential uses

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

            HorizontalDivider( // Changed Divider to HorizontalDivider
                modifier = Modifier.padding(bottom = 16.dp),
                thickness = DividerDefaults.Thickness, // Added thickness
                color = DividerDefaults.color // Added color
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "IMDb Rating: ", style = MaterialTheme.typography.bodyLarge)
                StarRating(imdbRating = detail.imdbRating)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = detail.imdbRating, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider( // Changed Divider to HorizontalDivider
                modifier = Modifier.padding(top = 16.dp),
                thickness = DividerDefaults.Thickness, // Added thickness
                color = DividerDefaults.color // Added color
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
                        viewModel.toggleFavorite()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (detail.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (detail.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (detail.isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                        tint = if (detail.isFavorite) Color.White else MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (detail.isFavorite) "Favorilerde" else "Favorilere Ekle",
                        color = if (detail.isFavorite) Color.White else MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.toggleWatched()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (detail.isWatched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        if (detail.isWatched) Icons.Filled.Check else Icons.Outlined.Check,
                        contentDescription = if (detail.isWatched) "İzlendi Olarak İşaretli" else "İzlendi Olarak İşaretle",
                        tint = if (detail.isWatched) Color.White else MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (detail.isWatched) "İzlendi" else "İzlendi Olarak İşaretle",
                        color = if (detail.isWatched) Color.White else MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

        } ?: run {
            Text(text = "Loading details...", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun StarRating(imdbRating: String) {
    val ratingFloat = imdbRating.toFloatOrNull() ?: 0f
    val ratingOutOf5 = ratingFloat / 2f
    val fullStars = floor(ratingOutOf5).toInt()
    val hasHalfStar = (ratingOutOf5 - fullStars) >= 0.5f

    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(fullStars) {
            Image(
                painter = painterResource(R.drawable.star_full),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }

        if (hasHalfStar) {
            Image(
                painter = painterResource(R.drawable.star_half),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }

        repeat(5 - fullStars - if (hasHalfStar) 1 else 0) {
            Image(
                painter = painterResource(R.drawable.star_empty),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
