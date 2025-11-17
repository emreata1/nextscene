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
import com.example.nextscene.data.MovieWatchlistManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MovieDetailViewModel(
    savedStateHandle: SavedStateHandle,
    authViewModel: AuthViewModel
) : ViewModel() {

    private val _rawMovieDetail = MutableStateFlow<MovieDetail?>(null)
    private var currentImdbID: String? = null

    // combine artık 4 parametre alıyor: Detay + Favoriler + İzlenenler + Watchlist
    val movieDetail: StateFlow<MovieDetail?> = combine(
        _rawMovieDetail,
        MovieWatchlistManager.favoriteMovieIds,
        MovieWatchlistManager.watchedMovieIds,
        MovieWatchlistManager.watchlistMovieIds // 1. YENİ: Watchlist akışını ekledik
    ) { rawDetail, favoriteIds, watchedIds, watchlistIds ->
        rawDetail?.copy(
            isFavorite = favoriteIds.contains(rawDetail.imdbID),
            isWatched = watchedIds.contains(rawDetail.imdbID),
            isInWatchlist = watchlistIds.contains(rawDetail.imdbID) // 2. YENİ: Watchlist durumunu kontrol et
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
                MovieWatchlistManager.setUserId(firebaseUser?.uid)
                currentImdbID?.let { imdbID ->
                    fetchMovieDetail(imdbID)
                }
            }
        }
    }

    private fun fetchMovieDetail(imdbID: String) {
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
            MovieWatchlistManager.toggleFavorite(currentDetail.imdbID)
        }
    }

    fun toggleWatched() {
        _rawMovieDetail.value?.let { currentDetail ->
            MovieWatchlistManager.toggleWatched(currentDetail.imdbID)
        }
    }

    // 3. YENİ: Watchlist Toggle Fonksiyonu
    fun toggleWatchlist() {
        _rawMovieDetail.value?.let { currentDetail ->
            MovieWatchlistManager.toggleWatchlist(currentDetail.imdbID)
        }
    }
}