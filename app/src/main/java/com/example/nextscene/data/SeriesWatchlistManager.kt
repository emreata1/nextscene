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

    // --- FAVORITES ---
    private val _favoriteSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSeriesIds: StateFlow<Set<String>> = _favoriteSeriesIds.asStateFlow()

    // --- WATCHED ---
    private val _watchedSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val watchedSeriesIds: StateFlow<Set<String>> = _watchedSeriesIds.asStateFlow()

    // --- WATCHLIST (DAHA SONRA İZLE) - YENİ ---
    private val _watchlistSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val watchlistSeriesIds: StateFlow<Set<String>> = _watchlistSeriesIds.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _currentUserId.collectLatest { userId ->
                if (userId != null) {
                    loadWatchlistFromFirestore(userId)
                } else {
                    _favoriteSeriesIds.value = emptySet()
                    _watchedSeriesIds.value = emptySet()
                    _watchlistSeriesIds.value = emptySet() // Reset watchlist
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
        // YENİ KOLEKSİYON REFERANSI
        val watchlistCollectionRef = firestore.collection("users").document(userId).collection("watchlistSeries")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Favorites Load
                val favoritesSnapshot = favoriteCollectionRef.get().await()
                _favoriteSeriesIds.value = favoritesSnapshot.documents.mapNotNull { it.id }.toSet()

                // Watched Load
                val watchedSnapshot = watchedCollectionRef.get().await()
                _watchedSeriesIds.value = watchedSnapshot.documents.mapNotNull { it.id }.toSet()

                // Watchlist Load (YENİ)
                val watchlistSnapshot = watchlistCollectionRef.get().await()
                _watchlistSeriesIds.value = watchlistSnapshot.documents.mapNotNull { it.id }.toSet()

            } catch (e: Exception) {
                println("Error loading series lists from subcollections: $e")
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
                    println("Error removing favorite series: $e")
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
                    println("Error adding favorite series: $e")
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
                    println("Error removing watched series: $e")
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
                    println("Error adding watched series: $e")
                }
        }
    }

    // --- YENİ: TOGGLE WATCHLIST FUNCTION ---
    fun toggleWatchlist(imdbID: String) {
        val userId = _currentUserId.value ?: return
        val watchlistDocRef = firestore.collection("users").document(userId).collection("watchlistSeries").document(imdbID)

        val currentWatchlist = _watchlistSeriesIds.value
        if (imdbID in currentWatchlist) {
            // Listeden Çıkar
            watchlistDocRef.delete()
                .addOnSuccessListener {
                    _watchlistSeriesIds.value = currentWatchlist - imdbID
                }
                .addOnFailureListener { e ->
                    println("Error removing watchlist series: $e")
                }
        } else {
            // Listeye Ekle
            val data = hashMapOf(
                "seriesId" to imdbID,
                "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            watchlistDocRef.set(data)
                .addOnSuccessListener {
                    _watchlistSeriesIds.value = currentWatchlist + imdbID
                }
                .addOnFailureListener { e ->
                    println("Error adding watchlist series: $e")
                }
        }
    }
}