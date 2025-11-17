package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.lifecycle.SavedStateHandle // Needed for initializer approach
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material.icons.filled.Movie
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
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                val posterUrl = detail.Poster
                val isPosterValid = posterUrl != "N/A" && posterUrl.isNotBlank()

                if (isPosterValid) {
                    // Resim varsa normal şekilde göster
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(posterUrl)
                                .crossfade(true)
                                .error(R.drawable.ic_broken_image) // Hata olursa (opsiyonel, yoksa silebilirsin)
                                .build()
                        ),
                        contentDescription = "Movie Poster",
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .height(200.dp)
                            .width(135.dp), // Sabit genişlik vermek görüntüyü korur (yaklaşık 2:3 oranı)
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Resim yoksa (N/A) Gri Kutu ve İkon göster
                    Box(
                        modifier = Modifier
                            .height(200.dp)
                            .width(135.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant), // Hafif gri arka plan
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie, // Film ikonu
                            contentDescription = "No Poster",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

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

            // -----------------------------------
            // Favorilere Ekle & İzlendi Butonları (1. SATIR)
            // -----------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- FAVORİ BUTONU ---
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        viewModel.toggleFavorite()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (detail.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (detail.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (detail.isFavorite) Color.White else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (detail.isFavorite) "Favorilerde" else "Ekle",
                        color = if (detail.isFavorite) Color.White else MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // --- İZLENDİ BUTONU ---
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        viewModel.toggleWatched()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (detail.isWatched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (detail.isWatched) Icons.Filled.Check else Icons.Outlined.Check,
                        contentDescription = null,
                        tint = if (detail.isWatched) Color.White else MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (detail.isWatched) "İzlendi" else "İzlendi Yap",
                        color = if (detail.isWatched) Color.White else MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) // Butonlar arası dikey boşluk

            // -----------------------------------
            // Daha Sonra İzle Butonu (2. SATIR - Tam Genişlik)
            // -----------------------------------
            // Not: detail.isWatchlist ve viewModel.toggleWatchlist() fonksiyonlarını
            // ViewModel ve Data class'ına eklemeyi unutma.
            Button(
                modifier = Modifier
                    .fillMaxWidth() // Ekranı kaplasın
                    .height(50.dp), // Diğerleriyle aynı yükseklik
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    viewModel.toggleWatchlist() // ViewModel'da bu fonksiyonu oluşturmalısın
                },
                colors = ButtonDefaults.buttonColors(
                    // Farklı bir renk tonu kullanabilirsin, örneğin SurfaceVariant veya PrimaryContainer
                    containerColor = if (detail.isInWatchlist) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (detail.isInWatchlist) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                // İkon Seçimi: Bookmark (Ayraç) genelde Watchlist için kullanılır
                Icon(
                    imageVector = if (detail.isInWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, // Import etmeyi unutma
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (detail.isInWatchlist) "Listemde Ekli" else "Daha Sonra İzle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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