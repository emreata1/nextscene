package com.example.nextscene.movieandseries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.data.SeriesWatchlistManager
import com.example.nextscene.network.NetworkModule
import com.example.nextscene.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeriesDetailViewModel(
    savedStateHandle: SavedStateHandle,
    authViewModel: AuthViewModel
) : ViewModel() {

    private val _rawMovieDetail = MutableStateFlow<MovieDetail?>(null)
    private var currentImdbID: String? = null

    val movieDetail: StateFlow<MovieDetail?> = combine(
        _rawMovieDetail,
        SeriesWatchlistManager.favoriteSeriesIds,
        SeriesWatchlistManager.watchedSeriesIds,
        SeriesWatchlistManager.watchlistSeriesIds
    ) { rawDetail, favoriteIds, watchedIds, watchlistIds ->
        rawDetail?.copy(
            isFavorite = favoriteIds.contains(rawDetail.imdbID),
            isWatched = watchedIds.contains(rawDetail.imdbID),
            isInWatchlist = watchlistIds.contains(rawDetail.imdbID)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000L),
        initialValue = null
    )

    init {
        currentImdbID = savedStateHandle.get<String>("imdbID")

        viewModelScope.launch {
            authViewModel.currentUser.collectLatest { firebaseUser ->
                SeriesWatchlistManager.setUserId(firebaseUser?.uid)
                currentImdbID?.let { imdbID ->
                    fetchSeriesDetail(imdbID)
                }
            }
        }
    }

    private fun fetchSeriesDetail(imdbID: String) {
        viewModelScope.launch {
            try {
                val response = NetworkModule.omdbApiService.getMovieDetail(imdbID, apiKey = NetworkModule.getApiKey())
                _rawMovieDetail.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite() {
        _rawMovieDetail.value?.let { currentDetail ->
            SeriesWatchlistManager.toggleFavorite(currentDetail.imdbID)
        }
    }

    fun toggleWatched() {
        _rawMovieDetail.value?.let { currentDetail ->
            SeriesWatchlistManager.toggleWatched(currentDetail.imdbID)
        }
    }

    fun toggleWatchlist() {
        _rawMovieDetail.value?.let { currentDetail ->
            SeriesWatchlistManager.toggleWatchlist(currentDetail.imdbID)
        }
    }
}