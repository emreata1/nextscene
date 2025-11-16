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

object WatchlistManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val _currentUserId = MutableStateFlow<String?>(null)

    private val _favoriteItemIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteItemIds: StateFlow<Set<String>> = _favoriteItemIds.asStateFlow()

    private val _watchedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val watchedItemIds: StateFlow<Set<String>> = _watchedItemIds.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _currentUserId.collectLatest { userId ->
                if (userId != null) {
                    loadWatchlistFromFirestore(userId)
                } else {
                    _favoriteItemIds.value = emptySet()
                    _watchedItemIds.value = emptySet()
                }
            }
        }
    }

    fun setUserId(userId: String?) {
        _currentUserId.value = userId
    }

    private fun loadWatchlistFromFirestore(userId: String) {
        val favoriteCollectionRef = firestore.collection("users").document(userId).collection("favorites")
        val watchedCollectionRef = firestore.collection("users").document(userId).collection("watched")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favoritesSnapshot = favoriteCollectionRef.get().await()
                _favoriteItemIds.value = favoritesSnapshot.documents.mapNotNull { it.id }.toSet()

                val watchedSnapshot = watchedCollectionRef.get().await()
                _watchedItemIds.value = watchedSnapshot.documents.mapNotNull { it.id }.toSet()
            } catch (e: Exception) {
                println("Error loading watchlist from subcollections: $e")
            }
        }
    }

    fun toggleFavorite(itemId: String) {
        val userId = _currentUserId.value ?: return
        val favoriteDocRef = firestore.collection("users").document(userId).collection("favorites").document(itemId)

        val currentFavorites = _favoriteItemIds.value
        if (itemId in currentFavorites) {
            favoriteDocRef.delete()
                .addOnSuccessListener {
                    _favoriteItemIds.value = currentFavorites - itemId
                }
                .addOnFailureListener { e ->
                    println("Error removing favorite item from subcollection: $e")
                }
        } else {
            val data = hashMapOf(
                "itemId" to itemId, // Add itemId as a field
                "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            favoriteDocRef.set(data)
                .addOnSuccessListener {
                    _favoriteItemIds.value = currentFavorites + itemId
                }
                .addOnFailureListener { e ->
                    println("Error adding favorite item to subcollection: $e")
                }
        }
    }

    fun toggleWatched(itemId: String) {
        val userId = _currentUserId.value ?: return
        val watchedDocRef = firestore.collection("users").document(userId).collection("watched").document(itemId)

        val currentWatched = _watchedItemIds.value
        if (itemId in currentWatched) {
            watchedDocRef.delete()
                .addOnSuccessListener {
                    _watchedItemIds.value = currentWatched - itemId
                }
                .addOnFailureListener { e ->
                    println("Error removing watched item from subcollection: $e")
                }
        } else {
            val data = hashMapOf(
                "itemId" to itemId, // Add itemId as a field
                "watchedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            watchedDocRef.set(data)
                .addOnSuccessListener {
                    _watchedItemIds.value = currentWatched + itemId
                }
                .addOnFailureListener { e ->
                    println("Error adding watched item to subcollection: $e")
                }
        }
    }

    fun isFavorite(itemId: String): Boolean {
        return itemId in _favoriteItemIds.value
    }

    fun isWatched(itemId: String): Boolean {
        return itemId in _watchedItemIds.value
    }
}
