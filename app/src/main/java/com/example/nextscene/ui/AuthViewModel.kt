package com.example.nextscene.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    suspend fun registerUser(email: String, password: String) {
        _authState.value = AuthState(isLoading = true)
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
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

    fun getCurrentUser() = auth.currentUser
}