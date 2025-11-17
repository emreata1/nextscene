package com.example.nextscene.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.nextscene.data.SeriesWatchlistManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SeriesDetailViewModel(
    savedStateHandle: SavedStateHandle,
    authViewModel: AuthViewModel
) : ViewModel() {

    private val _rawMovieDetail = MutableStateFlow<MovieDetail?>(null)
    private var currentImdbID: String? = null

    // combine artık 4 parametre alıyor: Detay + Favoriler + İzlenenler + Watchlist (YENİ)
    val movieDetail: StateFlow<MovieDetail?> = combine(
        _rawMovieDetail,
        SeriesWatchlistManager.favoriteSeriesIds,
        SeriesWatchlistManager.watchedSeriesIds,
        SeriesWatchlistManager.watchlistSeriesIds // YENİ: Watchlist akışını ekledik
    ) { rawDetail, favoriteIds, watchedIds, watchlistIds ->
        rawDetail?.copy(
            isFavorite = favoriteIds.contains(rawDetail.imdbID),
            isWatched = watchedIds.contains(rawDetail.imdbID),
            isInWatchlist = watchlistIds.contains(rawDetail.imdbID) // YENİ: Watchlist durumunu kontrol et
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
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

    // YENİ: Watchlist Toggle Fonksiyonu
    fun toggleWatchlist() {
        _rawMovieDetail.value?.let { currentDetail ->
            SeriesWatchlistManager.toggleWatchlist(currentDetail.imdbID)
        }
    }
}