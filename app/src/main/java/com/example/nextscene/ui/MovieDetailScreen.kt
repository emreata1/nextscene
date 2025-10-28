package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import kotlin.math.floor
import com.example.nextscene.R

@Composable
fun MovieDetailScreen(viewModel: MovieDetailViewModel = viewModel()) {
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
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(detail.Poster),
                        contentDescription = "Movie Poster",
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }


                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = detail.Title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = detail.Year,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detail.Runtime,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Text(text = detail.Genre, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = detail.Plot, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            Row( verticalAlignment = Alignment.CenterVertically) {
                Text(text = "IMDb Rating: ", style = MaterialTheme.typography.bodyLarge)
                StarRating(imdbRating = detail.imdbRating)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = detail.imdbRating, style = MaterialTheme.typography.bodyLarge)

            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            Text(text = "Rated: ${detail.Rated}", style = MaterialTheme.typography.bodyLarge)

            Text(text = "Director: ${detail.Director}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Actors: ${detail.Actors}", style = MaterialTheme.typography.bodyLarge)
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

    Row( verticalAlignment = Alignment.CenterVertically){
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

