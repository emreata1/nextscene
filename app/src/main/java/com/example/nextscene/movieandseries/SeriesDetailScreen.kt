package com.example.nextscene.movieandseries

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Movie // Varsayılan ikon için
import androidx.compose.foundation.background // Gri kutu arka planı için
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.request.ImageRequest
import com.example.nextscene.R
import com.example.nextscene.auth.AuthViewModel

@Composable
fun SeriesDetailScreen(
    // The authViewModel is now passed to the SeriesDetailViewModel via the factory
    authViewModel: AuthViewModel = viewModel()
) {
    // Create the SeriesDetailViewModel using a custom factory to inject AuthViewModel
    val viewModel: SeriesDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                // Access SavedStateHandle from the initializer scope
                SeriesDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    authViewModel = authViewModel
                )
            }
        }
    )

    val movieDetail by viewModel.movieDetail.collectAsState()

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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start // ÖNEMLİ: İçindekileri sola (başa) yasla
            ) {
                val posterUrl = detail.Poster
                val isPosterValid = posterUrl != "N/A" && posterUrl?.isNotBlank() == true

                if (isPosterValid) {
                    // Resim yükleyici

                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(posterUrl)
                                    .crossfade(true)
                                    .error(R.drawable.ic_broken_image)
                                    .build()
                            ),
                            contentDescription = "Movie Poster",
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .height(200.dp)
                                .width(135.dp),
                            contentScale = ContentScale.Crop
                        )


                } else {
                    // Resim yoksa (N/A) Gri Kutu ve İkon
                    Box(
                        modifier = Modifier
                            .height(200.dp)
                            .width(135.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = "No Poster",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }


                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    detail.Title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    detail.Year?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                    Spacer(modifier = Modifier.height(8.dp))
                    detail.Runtime?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                }
            }

            detail.Genre?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            Spacer(modifier = Modifier.height(16.dp))
            detail.Plot?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "IMDb Rating: ", style = MaterialTheme.typography.bodyLarge)
                detail.imdbRating?.let { StarRating(imdbRating = it) }
                Spacer(modifier = Modifier.width(8.dp))
                detail.imdbRating?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
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
            // 1. SATIR: Favorilere Ekle & İzlendi Butonları
            // -----------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Butonlar arası boşluk
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
                        text = if (detail.isFavorite) "Favorilerde" else "Favorilere Ekle",
                        color = if (detail.isFavorite) Color.White else MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) // Dikey boşluk

            // -----------------------------------
            // 2. SATIR: Daha Sonra İzle Butonu
            // -----------------------------------
            Button(
                modifier = Modifier
                    .fillMaxWidth() // Tam genişlik
                    .height(50.dp), // Diğerleriyle aynı yükseklik
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    viewModel.toggleWatchlist() // Bu fonksiyonu ViewModel'a eklemelisin
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (detail.isInWatchlist) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (detail.isInWatchlist) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (detail.isInWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
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