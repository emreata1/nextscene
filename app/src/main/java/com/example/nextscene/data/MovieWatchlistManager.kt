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

object MovieWatchlistManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val _currentUserId = MutableStateFlow<String?>(null)

    private val _favoriteMovieIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteMovieIds: StateFlow<Set<String>> = _favoriteMovieIds.asStateFlow()

    private val _watchedMovieIds = MutableStateFlow<Set<String>>(emptySet())
    val watchedMovieIds: StateFlow<Set<String>> = _watchedMovieIds.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _currentUserId.collectLatest { userId ->
                if (userId != null) {
                    loadWatchlistFromFirestore(userId)
                } else {
                    _favoriteMovieIds.value = emptySet()
                    _watchedMovieIds.value = emptySet()
                }
            }
        }
    }

    fun setUserId(userId: String?) {
        _currentUserId.value = userId
    }

    private fun loadWatchlistFromFirestore(userId: String) {
        val favoriteCollectionRef = firestore.collection("users").document(userId).collection("favoriteMovies")
        val watchedCollectionRef = firestore.collection("users").document(userId).collection("watchedMovies")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favoritesSnapshot = favoriteCollectionRef.get().await()
                _favoriteMovieIds.value = favoritesSnapshot.documents.mapNotNull { it.id }.toSet()

                val watchedSnapshot = watchedCollectionRef.get().await()
                _watchedMovieIds.value = watchedSnapshot.documents.mapNotNull { it.id }.toSet()
            } catch (e: Exception) {
                println("Error loading movie watchlist from subcollections: $e")
            }
        }
    }

    fun toggleFavorite(imdbID: String) {
        val userId = _currentUserId.value ?: return
        val favoriteDocRef = firestore.collection("users").document(userId).collection("favoriteMovies").document(imdbID)

        val currentFavorites = _favoriteMovieIds.value
        if (imdbID in currentFavorites) {
            favoriteDocRef.delete()
                .addOnSuccessListener {
                    _favoriteMovieIds.value = currentFavorites - imdbID
                }
                .addOnFailureListener { e ->
                    println("Error removing favorite movie from subcollection: $e")
                }
        } else {
            val data = hashMapOf(
                "movieId" to imdbID, // Add movieId as a field
                "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            favoriteDocRef.set(data)
                .addOnSuccessListener {
                    _favoriteMovieIds.value = currentFavorites + imdbID
                }
                .addOnFailureListener { e ->
                    println("Error adding favorite movie to subcollection: $e")
                }
        }
    }

    fun toggleWatched(imdbID: String) {
        val userId = _currentUserId.value ?: return
        val watchedDocRef = firestore.collection("users").document(userId).collection("watchedMovies").document(imdbID)

        val currentWatched = _watchedMovieIds.value
        if (imdbID in currentWatched) {
            watchedDocRef.delete()
                .addOnSuccessListener {
                    _watchedMovieIds.value = currentWatched - imdbID
                }
                .addOnFailureListener { e ->
                    println("Error removing watched movie from subcollection: $e")
                }
        } else {
            val data = hashMapOf(
                "movieId" to imdbID, // Add movieId as a field
                "watchedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            watchedDocRef.set(data)
                .addOnSuccessListener {
                    _watchedMovieIds.value = currentWatched + imdbID
                }
                .addOnFailureListener { e ->
                    println("Error adding watched movie to subcollection: $e")
                }
        }
    }

    fun isFavorite(imdbID: String): Boolean {
        return imdbID in _favoriteMovieIds.value
    }

    fun isWatched(imdbID: String): Boolean {
        return imdbID in _watchedMovieIds.value
    }
}
