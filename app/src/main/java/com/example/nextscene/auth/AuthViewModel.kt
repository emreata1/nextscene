package com.example.nextscene.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextscene.profile.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query


data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class UserData(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val role: String = "",
    val createdAt: Timestamp? = null,
    val profileImageUrl: String = "",
    val followerCount: Long = 0,
    val followingCount: Long = 0
)


class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _searchResults = MutableStateFlow<List<UserData>>(emptyList())
    val searchResults: StateFlow<List<UserData>> = _searchResults

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())
    val allPosts: StateFlow<List<Post>> = _allPosts

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

    fun searchUsers(query: String) {


        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("username", query)
                    .whereLessThanOrEqualTo("username", query + "\uf8ff")
                    .limit(10)
                    .get()
                    .await()


                val users = snapshot.toObjects(UserData::class.java)
                val currentUid = _currentUser.value?.uid
                _searchResults.value = users.filter { it.uid != currentUid }

            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
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
                "role" to "user",
                "profileImageUrl" to "",
                "followerCount" to 0L, // İlk değer
                "followingCount" to 0L  // İlk değer
            )

            db.collection("users").document(uid).set(userData).await() // <-- Buradaki UID, kuraldaki {userId} ile eşleşmeli            _authState.value = AuthState(isLoading = false, isSuccess = true)

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


    suspend fun updateUserData(updatedData: Map<String, Any>) {
        _authState.value = AuthState(isLoading = true)
        val uid = _currentUser.value?.uid ?: return
        try {
            db.collection("users").document(uid).update(updatedData).await()
            fetchUserData(uid)
            _authState.value = AuthState(isLoading = false, isSuccess = true)
        } catch (e: Exception) {
            _authState.value = AuthState(isLoading = false, errorMessage = e.message)
        }
    }

    suspend fun isUserFollowing(targetUid: String): Boolean {
        val currentUid = _currentUser.value?.uid ?: return false
        val doc = db.collection("users").document(currentUid)
            .collection("following").document(targetUid).get().await()
        return doc.exists()
    }

    suspend fun followUser(targetUid: String) {
        val currentUid = _currentUser.value?.uid ?: return

        val batch = db.batch()

        val myFollowingRef = db.collection("users").document(currentUid)
            .collection("following").document(targetUid)
        batch.set(myFollowingRef, hashMapOf("followedAt" to FieldValue.serverTimestamp()))

        val targetFollowerRef = db.collection("users").document(targetUid)
            .collection("followers").document(currentUid)
        batch.set(targetFollowerRef, hashMapOf("followedAt" to FieldValue.serverTimestamp()))

        val myUserRef = db.collection("users").document(currentUid)
        batch.update(myUserRef, "followingCount", FieldValue.increment(1))

        val targetUserRef = db.collection("users").document(targetUid)
        batch.update(targetUserRef, "followerCount", FieldValue.increment(1))

        batch.commit().await()
    }

    suspend fun unfollowUser(targetUid: String) {
        val currentUid = _currentUser.value?.uid ?: return

        val batch = db.batch()

        val myFollowingRef = db.collection("users").document(currentUid)
            .collection("following").document(targetUid)
        batch.delete(myFollowingRef)

        val targetFollowerRef = db.collection("users").document(targetUid)
            .collection("followers").document(currentUid)
        batch.delete(targetFollowerRef)

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
                .get(AggregateSource.SERVER)
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


    fun fetchAllPosts() {
        viewModelScope.launch {
            try {
                val postsSnapshot = FirebaseFirestore.getInstance().collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()

                _allPosts.value = postsSnapshot.toObjects(Post::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun resetState() { _authState.value = AuthState() }
    fun getCurrentUser() = _currentUser.value

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _userData.value = null
        _authState.value = AuthState()
    }
}