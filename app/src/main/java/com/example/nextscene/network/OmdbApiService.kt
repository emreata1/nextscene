package com.example.nextscene.network

import com.example.nextscene.data.OmdbResponse
import com.example.nextscene.data.MovieDetail
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApiService {
    @GET("/")
    suspend fun searchMovies(
        @Query("s") query: String,
        @Query("type") type: String = "series",
        @Query("apikey") apiKey: String
    ): OmdbResponse

    @GET("/")
    suspend fun getMovieDetail(
        @Query("i") imdbID: String,
        @Query("plot") plot: String = "full",
        @Query("apikey") apiKey: String
    ): MovieDetail
}