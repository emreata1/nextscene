package com.example.nextscene.movieandseries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextscene.data.Movie
import com.example.nextscene.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoviesViewModel : ViewModel() {

    private val _films = MutableStateFlow<List<Movie>>(emptyList())
    val films: StateFlow<List<Movie>> = _films

    init {
        fetchFilms("star")
    }

    fun fetchFilms(query: String) {
        viewModelScope.launch {
            try {
                val response = NetworkModule.omdbApiService.searchMovies(query, type = "movie", apiKey = NetworkModule.getApiKey())
                _films.value = response.Search
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}