package com.example.nextscene.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import com.google.firebase.firestore.AggregateSource

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

// Veri sınıfın (Eski, sade hali)
data class UserData(
    val uid: String = "",
    val email: String = "",
    val username: String = "", // Sadece username var
    val name: String = "",
    val surname: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val role: String = "",
    val createdAt: Timestamp? = null,
    val profileImageUrl: String = ""
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _searchResults = MutableStateFlow<List<UserData>>(emptyList())
    val searchResults: StateFlow<List<UserData>> = _searchResults

    init {
        auth.addAuthStateListener {
            _currentUser.value = it.currentUser
            if (it.currentUser != null) {
                it.currentUser?.let { it1 -> fetchUserData(it1.uid) }
            } else {
                _userData.value = null
            }
        }
    }

    fun fetchUserData(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                _userData.value = doc.toObject(UserData::class.java)
            } catch (e: Exception) {
                _userData.value = null
            }
        }
    }

    // --- SADELEŞTİRİLMİŞ ARAMA FONKSİYONU ---
    // Hata ayıklamak için güncellenmiş DEDEKTİF fonksiyon
    fun searchUsers(query: String) {
        // 1. Fonksiyon çalışıyor mu?
        Log.e("SearchDebug", "--------------------------------------------------")
        Log.e("SearchDebug", "ARAMA BAŞLADI. Aranan kelime: '$query'")

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                // 2. Sorgu gönderiliyor...
                val snapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("username", query)
                    .whereLessThanOrEqualTo("username", query + "\uf8ff")
                    .limit(10)
                    .get()
                    .await()

                // 3. SONUÇ ANALİZİ
                if (snapshot.isEmpty) {
                    Log.e("SearchDebug", "⚠️ SONUÇ BULUNAMADI!")
                    Log.e("SearchDebug", "Veritabanında 'username' alanı '$query' ile başlayan bir kayıt yok.")
                    Log.e("SearchDebug", "İPUCU: Büyük/küçük harf veya boşluk hatası olabilir.")
                } else {
                    Log.e("SearchDebug", "✅ BAŞARILI! ${snapshot.size()} kullanıcı bulundu.")
                    snapshot.documents.forEach { doc ->
                        // Veritabanından gelen gerçek veriyi yazdır
                        val gelenUsername = doc.getString("username")
                        Log.e("SearchDebug", "-> Bulunan ID: ${doc.id}")
                        Log.e("SearchDebug", "-> Bulunan Username: '$gelenUsername'")
                    }
                }

                val users = snapshot.toObjects(UserData::class.java)
                val currentUid = _currentUser.value?.uid
                _searchResults.value = users.filter { it.uid != currentUid }

            } catch (e: Exception) {
                // 4. Teknik Hata var mı?
                Log.e("SearchDebug", "🚨 KRİTİK HATA OLUŞTU:", e)
                _searchResults.value = emptyList()
            }
            Log.e("SearchDebug", "--------------------------------------------------")
        }
    }
    // ----------------------------------------

    // ... (updateUserData, registerUser, loginUser vb. diğer fonksiyonlar değişmedi) ...
    // Not: registerUser içindeki 'username_lowercase' kısmını sildim.

    suspend fun updateUserData(updatedData: Map<String, Any>) {
        _authState.value = AuthState(isLoading = true)
        val uid = _currentUser.value?.uid
        if (uid == null) return
        try {
            db.collection("users").document(uid).update(updatedData).await()
            fetchUserData(uid)
            _authState.value = AuthState(isLoading = false, isSuccess = true)
        } catch (e: Exception) {
            _authState.value = AuthState(isLoading = false, errorMessage = e.message)
        }
    }

    suspend fun registerUser(email: String, password: String, username: String) {
        _authState.value = AuthState(isLoading = true)
        try {
            val usernameQuery = db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!usernameQuery.isEmpty) throw Exception("Bu kullanıcı adı zaten alınmış.")

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
                "role" to "",
                "profileImageUrl" to ""
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

    fun resetState() { _authState.value = AuthState() }
    fun getCurrentUser() = _currentUser.value

    // ... (Firestore film/dizi fonksiyonları aynı kalsın) ...
    suspend fun addWatchedMovie(uid: String, movieId: String) {
        val data = hashMapOf("movieId" to movieId, "watchedAt" to FieldValue.serverTimestamp())
        db.collection("users").document(uid).collection("watchedMovies").add(data).await()
    }
    suspend fun addFavoriteMovie(uid: String, movieId: String) {
        val data = hashMapOf("movieId" to movieId, "addedAt" to FieldValue.serverTimestamp())
        db.collection("users").document(uid).collection("favoriteMovies").add(data).await()
    }
    suspend fun addWatchedSeries(uid: String, seriesId: String) {
        val data = hashMapOf("seriesId" to seriesId, "watchedAt" to FieldValue.serverTimestamp())
        db.collection("users").document(uid).collection("watchedSeries").add(data).await()
    }
    suspend fun addFavoriteSeries(uid: String, seriesId: String) {
        val data = hashMapOf("seriesId" to seriesId, "addedAt" to FieldValue.serverTimestamp())
        db.collection("users").document(uid).collection("favoriteSeries").add(data).await()
    }
    suspend fun getFavoriteMovies(uid: String): List<String> {
        val snapshot = db.collection("users").document(uid).collection("favoriteMovies").get().await()
        return snapshot.documents.map { it.getString("movieId") ?: "" }
    }

    // ... (Mevcut kodların altına ekle) ...

    // Bir kullanıcının diğerini takip edip etmediğini kontrol et
    suspend fun isUserFollowing(targetUid: String): Boolean {
        val currentUid = _currentUser.value?.uid ?: return false
        val doc = db.collection("users").document(currentUid)
            .collection("following").document(targetUid).get().await()
        return doc.exists()
    }

    // Takip Etme İşlemi
    suspend fun followUser(targetUid: String) {
        val currentUid = _currentUser.value?.uid ?: return

        val batch = db.batch()

        // 1. BENİM 'following' koleksiyonuma hedef kullanıcıyı ekle
        val myFollowingRef = db.collection("users").document(currentUid)
            .collection("following").document(targetUid)
        batch.set(myFollowingRef, hashMapOf("followedAt" to FieldValue.serverTimestamp()))

        // 2. HEDEFİN 'followers' koleksiyonuna beni ekle
        val targetFollowerRef = db.collection("users").document(targetUid)
            .collection("followers").document(currentUid)
        batch.set(targetFollowerRef, hashMapOf("followedAt" to FieldValue.serverTimestamp()))

        // 3. Sayaçları Güncelle (Increment)
        val myUserRef = db.collection("users").document(currentUid)
        batch.update(myUserRef, "followingCount", FieldValue.increment(1))

        val targetUserRef = db.collection("users").document(targetUid)
        batch.update(targetUserRef, "followerCount", FieldValue.increment(1))

        batch.commit().await()
    }

    // Takipten Çıkma İşlemi
    suspend fun unfollowUser(targetUid: String) {
        val currentUid = _currentUser.value?.uid ?: return

        val batch = db.batch()

        // 1. Koleksiyonlardan sil
        val myFollowingRef = db.collection("users").document(currentUid)
            .collection("following").document(targetUid)
        batch.delete(myFollowingRef)

        val targetFollowerRef = db.collection("users").document(targetUid)
            .collection("followers").document(currentUid)
        batch.delete(targetFollowerRef)

        // 2. Sayaçları Güncelle (Decrement)
        val myUserRef = db.collection("users").document(currentUid)
        batch.update(myUserRef, "followingCount", FieldValue.increment(-1))

        val targetUserRef = db.collection("users").document(targetUid)
        batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))

        batch.commit().await()
    }
    suspend fun getFollowerCountFromSubcollection(uid: String): Long {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("followers")
                .count()
                .get(AggregateSource.SERVER) // Sunucudan say
                .await()
            snapshot.count
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getFollowingCountFromSubcollection(uid: String): Long {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("following")
                .count()
                .get(AggregateSource.SERVER)
                .await()
            snapshot.count
        } catch (e: Exception) {
            0
        }
    }


}
