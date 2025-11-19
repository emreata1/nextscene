@file:Suppress("PropertyName")

package com.example.nextscene.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class OmdbResponse(
    val Search: List<Movie> = emptyList(),
    val totalResults: String,
    val Response: String
)

