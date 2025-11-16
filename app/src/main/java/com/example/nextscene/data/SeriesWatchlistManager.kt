package com.example.nextscene.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object SeriesWatchlistManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val _currentUserId = MutableStateFlow<String?>(null)

    private val _favoriteSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSeriesIds: StateFlow<Set<String>> = _favoriteSeriesIds.asStateFlow()

    private val _watchedSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val watchedSeriesIds: StateFlow<Set<String>> = _watchedSeriesIds.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _currentUserId.collectLatest { userId ->
                if (userId != null) {
                    loadWatchlistFromFirestore(userId)
                } else {
                    _favoriteSeriesIds.value = emptySet()
                    _watchedSeriesIds.value = emptySet()
                }
            }
        }
    }

    fun setUserId(userId: String?) {
        _currentUserId.value = userId
    }

    private fun loadWatchlistFromFirestore(userId: String) {
        val favoriteCollectionRef = firestore.collection("users").document(userId).collection("favoriteSeries")
        val watchedCollectionRef = firestore.collection("users").document(userId).collection("watchedSeries")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favoritesSnapshot = favoriteCollectionRef.get().await()
                _favoriteSeriesIds.value = favoritesSnapshot.documents.mapNotNull { it.id }.toSet()

                val watchedSnapshot = watchedCollectionRef.get().await()
                _watchedSeriesIds.value = watchedSnapshot.documents.mapNotNull { it.id }.toSet()
            } catch (e: Exception) {
                println("Error loading series watchlist from subcollections: $e")
            }
        }
    }

    fun toggleFavorite(imdbID: String) {
        val userId = _currentUserId.value ?: return
        val favoriteDocRef = firestore.collection("users").document(userId).collection("favoriteSeries").document(imdbID)

        val currentFavorites = _favoriteSeriesIds.value
        if (imdbID in currentFavorites) {
            favoriteDocRef.delete()
                .addOnSuccessListener {
                    _favoriteSeriesIds.value = currentFavorites - imdbID
                }
                .addOnFailureListener { e ->
                    println("Error removing favorite series from subcollection: $e")
                }
        } else {
            val data = hashMapOf(
                "seriesId" to imdbID,
                "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            favoriteDocRef.set(data)
                .addOnSuccessListener {
                    _favoriteSeriesIds.value = currentFavorites + imdbID
                }
                .addOnFailureListener { e ->
                    println("Error adding favorite series to subcollection: $e")
                }
        }
    }

    fun toggleWatched(imdbID: String) {
        val userId = _currentUserId.value ?: return
        val watchedDocRef = firestore.collection("users").document(userId).collection("watchedSeries").document(imdbID)

        val currentWatched = _watchedSeriesIds.value
        if (imdbID in currentWatched) {
            watchedDocRef.delete()
                .addOnSuccessListener {
                    _watchedSeriesIds.value = currentWatched - imdbID
                }
                .addOnFailureListener { e ->
                    println("Error removing watched series from subcollection: $e")
                }
        } else {
            val data = hashMapOf(
                "seriesId" to imdbID,
                "watchedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            watchedDocRef.set(data)
                .addOnSuccessListener {
                    _watchedSeriesIds.value = currentWatched + imdbID
                }
                .addOnFailureListener { e ->
                    println("Error adding watched series to subcollection: $e")
                }
        }
    }

    fun isFavorite(imdbID: String): Boolean {
        return imdbID in _favoriteSeriesIds.value
    }

    fun isWatched(imdbID: String): Boolean {
        return imdbID in _watchedSeriesIds.value
    }
}
