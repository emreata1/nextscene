package com.example.nextscene.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _movieDetail = MutableStateFlow<MovieDetail?>(null)
    val movieDetail: StateFlow<MovieDetail?> = _movieDetail

    init {
        savedStateHandle.get<String>("imdbID")?.let { imdbID ->
            fetchSeriesDetail(imdbID)
        }
    }

    private fun fetchSeriesDetail(imdbID: String) {
        viewModelScope.launch {
            try {
                val response = NetworkModule.omdbApiService.getMovieDetail(imdbID, apiKey = NetworkModule.getApiKey())
                _movieDetail.value = response
            } catch (e: Exception) {
                // Hata yönetimi
                e.printStackTrace()
            }
        }
    }
}