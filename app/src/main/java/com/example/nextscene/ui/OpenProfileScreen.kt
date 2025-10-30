package com.example.nextscene.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenProfileScreen(
    viewModel: AuthViewModel = viewModel()
) {
    val db = FirebaseFirestore.getInstance()

    // 🔹 Oturum açmış kullanıcıyı dinliyoruz
    val currentUser by viewModel.currentUser.collectAsState()

    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var watchedMovies by remember { mutableStateOf(listOf<String>()) }
    var favoriteMovies by remember { mutableStateOf(listOf<String>()) }
    var watchedSeries by remember { mutableStateOf(listOf<String>()) }
    var favoriteSeries by remember { mutableStateOf(listOf<String>()) }

    // 🔹 Kullanıcı değiştiğinde veriyi yeniden çek
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        val userDoc = db.collection("users").document(uid).get().await()
        userData = userDoc.data

        watchedMovies = db.collection("users").document(uid)
            .collection("watchedMovies").get().await().documents
            .mapNotNull { it.getString("movieId") }

        favoriteMovies = db.collection("users").document(uid)
            .collection("favoriteMovies").get().await().documents
            .mapNotNull { it.getString("movieId") }

        watchedSeries = db.collection("users").document(uid)
            .collection("watchedSeries").get().await().documents
            .mapNotNull { it.getString("seriesId") }

        favoriteSeries = db.collection("users").document(uid)
            .collection("favoriteSeries").get().await().documents
            .mapNotNull { it.getString("seriesId") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userData?.get("username") as? String ?: "Profil") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            if (currentUser == null) {
                item { Text("Lütfen giriş yapın.") }
            } else {
                item { Text("İzlenen Filmler: ${watchedMovies.joinToString(", ")}") }
                item { Text("Favori Filmler: ${favoriteMovies.joinToString(", ")}") }
                item { Text("İzlenen Diziler: ${watchedSeries.joinToString(", ")}") }
                item { Text("Favori Diziler: ${favoriteSeries.joinToString(", ")}") }
            }
        }
    }
}
