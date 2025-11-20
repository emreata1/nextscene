package com.example.nextscene.movieandseries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextscene.data.Movie
import com.example.nextscene.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesViewModel : ViewModel() {

    private val _series = MutableStateFlow<List<Movie>>(emptyList())
    val series: StateFlow<List<Movie>> = _series

    init {
        fetchSeries("spider")
    }

    fun fetchSeries(query: String) {
        viewModelScope.launch {
            try {
                val response = NetworkModule.omdbApiService.searchMovies(query, apiKey = NetworkModule.getApiKey())
                _series.value = response.Search
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}