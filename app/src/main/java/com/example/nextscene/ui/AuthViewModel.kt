package com.example.nextscene.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    // 🔹 Kullanıcı oturum durumunu dinlemek için
    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser

    init {
        // 🔹 Kullanıcı giriş/çıkış yaptığında otomatik günceller
        auth.addAuthStateListener {
            _currentUser.value = it.currentUser
        }
    }

    suspend fun registerUser(email: String, password: String, username: String) {
        _authState.value = AuthState(isLoading = true)
        try {
            val usernameQuery = db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!usernameQuery.isEmpty) {
                throw Exception("Bu kullanıcı adı zaten alınmış.")
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID alınamadı")

            val userData = hashMapOf(
                "uid" to uid,
                "email" to email,
                "username" to username,
                "createdAt" to FieldValue.serverTimestamp(),
                "name" to "",
                "surname" to "",
                "phoneNumber" to "",
                "bio" to "",
                "role" to ""
            )

            db.collection("users").document(uid).set(userData).await()

            _authState.value = AuthState(isLoading = false, isSuccess = true)

        } catch (e: Exception) {
            _authState.value = AuthState(isLoading = false, errorMessage = e.message)
        }
    }

    suspend fun loginUser(email: String, password: String) {
        _authState.value = AuthState(isLoading = true)
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            _authState.value = AuthState(isLoading = false, isSuccess = true)
        } catch (e: Exception) {
            _authState.value = AuthState(isLoading = false, errorMessage = e.message)
        }
    }

    fun resetState() {
        _authState.value = AuthState()
    }

    // 🔹 Giriş yapmış kullanıcıyı verir
    fun getCurrentUser() = _currentUser.value

    // 🔹 Firestore işlemleri
    suspend fun addWatchedMovie(uid: String, movieId: String) {
        val data = hashMapOf(
            "movieId" to movieId,
            "watchedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .collection("watchedMovies").add(data).await()
    }

    suspend fun addFavoriteMovie(uid: String, movieId: String) {
        val data = hashMapOf(
            "movieId" to movieId,
            "addedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .collection("favoriteMovies").add(data).await()
    }

    suspend fun addWatchedSeries(uid: String, seriesId: String) {
        val data = hashMapOf(
            "seriesId" to seriesId,
            "watchedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .collection("watchedSeries").add(data).await()
    }

    suspend fun addFavoriteSeries(uid: String, seriesId: String) {
        val data = hashMapOf(
            "seriesId" to seriesId,
            "addedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .collection("favoriteSeries").add(data).await()
    }

    suspend fun getFavoriteMovies(uid: String): List<String> {
        val snapshot = db.collection("users").document(uid)
            .collection("favoriteMovies").get().await()

        return snapshot.documents.map { it.getString("movieId") ?: "" }
    }
}
