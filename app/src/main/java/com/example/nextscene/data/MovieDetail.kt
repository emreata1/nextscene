@file:Suppress("PropertyName")

package com.example.nextscene.data

import android.annotation.SuppressLint
import android.text.BoringLayout
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Rating(
    val Source: String,
    val Value: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MovieDetail(
    val Title: String?,        // Başlık bazen eksik gelebilir
    val Year: String?,
    val Rated: String?,
    val Released: String?,
    val Runtime: String?,
    val Genre: String?,
    val Director: String?,
    val Writer: String?,
    val Actors: String?,
    val Plot: String?,
    val Language: String?,
    val Country: String?,
    val Awards: String?,
    val Poster: String?,       // API'den "N/A" veya null gelebilir. Çökmenin en olası sebebi buydu.
    val Ratings: List<Rating>? = emptyList(), // Artık null gelirse sorun olmaz    val Metascore: String? = null,    // Varsayılan değeri String? yaparken null olarak ayarlayalım
    val imdbRating: String?,
    val imdbVotes: String?,
    val imdbID: String,        // Bu genellikle non-null'dır
    val Type: String?,
    val DVD: String? = null,
    val BoxOffice: String? = null,
    val Production: String? = null,
    val Website: String? = null,
    val Response: String?,

    // UI durumları için bu alanlar sorun yaratmaz
    var isFavorite: Boolean = false,
    var isWatched: Boolean = false,
    var isInWatchlist: Boolean = false
)


