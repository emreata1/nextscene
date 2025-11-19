package com.example.nextscene.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nextscene.data.MovieDetail
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.nextscene.network.NetworkModule
import com.example.nextscene.auth.AuthViewModel
import com.example.nextscene.auth.UserData

data class Post(
    val postId: String = "",
    val userId: String = "",
    val mediaId: String = "",
    val mediaTitle: String = "",
    val mediaPoster: String = "",
    val mediaType: String = "",
    val title: String = "",
    val reviewText: String = "",
    val rating: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    targetUid: String? = null
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser by authViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    val profileUid = targetUid ?: currentUser?.uid

    var displayUser by remember { mutableStateOf<UserData?>(null) }
    var realFollowerCount by remember { mutableLongStateOf(0) }
    var realFollowingCount by remember { mutableLongStateOf(0) }

    var watchedMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var favoriteMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var watchlistMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var watchedSeries by remember { mutableStateOf(listOf<MovieDetail>()) }
    var favoriteSeries by remember { mutableStateOf(listOf<MovieDetail>()) }
    var watchlistSeries by remember { mutableStateOf(listOf<MovieDetail>()) }

    var isLoading by remember { mutableStateOf(true) }

    var isFollowing by remember { mutableStateOf(false) }
    var followLoading by remember { mutableStateOf(false) }

    var userPosts by remember { mutableStateOf(listOf<Post>()) }
    var showNewPostDialog by remember { mutableStateOf(false) } // Yeni Gönderi Dialog'u

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Gönderiler", "Listeler")


    val refreshPosts: () -> Unit = {
        scope.launch {
            if (profileUid != null) {
                try {
                    val postsSnapshot = db.collection("posts")
                        .whereEqualTo("userId", profileUid)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get().await()
                    userPosts = postsSnapshot.toObjects(Post::class.java)
                } catch (e: Exception) {
                    println("Gönderi Yenileme Hatası: ${e.message}")
                }
            }
        }
    }


    LaunchedEffect(profileUid) {
        if (profileUid != null) {
            isLoading = true
            try {
                val userDoc = db.collection("users").document(profileUid).get().await()
                displayUser = userDoc.toObject(UserData::class.java)

                val followerTask = async { authViewModel.getFollowerCountFromSubcollection(profileUid) }
                val followingTask = async { authViewModel.getFollowingCountFromSubcollection(profileUid) }

                realFollowerCount = followerTask.await()
                realFollowingCount = followingTask.await()

                if (currentUser != null && profileUid != currentUser?.uid) {
                    isFollowing = authViewModel.isUserFollowing(profileUid)
                }

                val userRef = db.collection("users").document(profileUid)
                val wM_Ids = userRef.collection("watchedMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val fM_Ids = userRef.collection("favoriteMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val wlM_Ids = userRef.collection("watchlistMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val wS_Ids = userRef.collection("watchedSeries").get().await().documents.mapNotNull { it.getString("seriesId") }
                val fS_Ids = userRef.collection("favoriteSeries").get().await().documents.mapNotNull { it.getString("seriesId") }
                val wlS_Ids = userRef.collection("watchlistSeries").get().await().documents.mapNotNull { it.getString("seriesId") }

                val apiKey = "b9bd48a6"

                watchedMovies = wM_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                favoriteMovies = fM_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                watchlistMovies = wlM_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                watchedSeries = wS_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                favoriteSeries = fS_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                watchlistSeries = wlS_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()

                val postsSnapshot = db.collection("posts")
                    .whereEqualTo("userId", profileUid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().await()
                userPosts = postsSnapshot.toObjects(Post::class.java)

            } catch (e: Exception) {
                println("Profil Veri Çekme Hatası: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (profileUid == currentUser?.uid && !isLoading) {
                FloatingActionButton(onClick = { showNewPostDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Yeni Gönderi Ekle")
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (displayUser == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Kullanıcı bulunamadı.")
            }
        } else {
            val combinedWatched = (watchedMovies + watchedSeries).shuffled()
            val combinedFavorites = (favoriteMovies + favoriteSeries).shuffled()
            val combinedWatchlist = (watchlistMovies + watchlistSeries).shuffled()
            val totalWatchedCount = combinedWatched.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    ProfileHeader(
                        userData = displayUser,
                        watchedCount = totalWatchedCount,
                        followerCount = realFollowerCount.toInt(),
                        followingCount = realFollowingCount.toInt(),
                        navController = navController,
                        onFollowClick = { type ->
                            if (displayUser != null) {
                                navController.navigate("followList/${displayUser!!.uid}/$type")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (profileUid != currentUser?.uid && profileUid != null) {
                        Button(
                            onClick = {
                                scope.launch {
                                    followLoading = true
                                    if (isFollowing) {
                                        authViewModel.unfollowUser(profileUid)
                                        isFollowing = false
                                    } else {
                                        authViewModel.followUser(profileUid)
                                        isFollowing = true
                                    }
                                    followLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            enabled = !followLoading
                        ) {
                            if (followLoading) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } else { Text(text = if (isFollowing) "Takipten Çık" else "Takip Et") }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    when (selectedTab) {
                        0 -> {
                            PostsSection(
                                posts = userPosts,
                                onPostClick = { postId ->
                                }
                            )
                        }
                        1 -> {
                            ListContent(
                                profileUid = profileUid!!,
                                navController = navController,
                                combinedWatched = combinedWatched,
                                combinedFavorites = combinedFavorites,
                                combinedWatchlist = combinedWatchlist
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showNewPostDialog && currentUser != null) {
        NewPostDialog(
            currentUserId = currentUser!!.uid,
            onDismiss = { showNewPostDialog = false },
            onPostCreated = {
                showNewPostDialog = false
                refreshPosts()
            }
        )
    }
}

@Composable
fun ListContent(
    profileUid: String,
    navController: NavController,
    combinedWatched: List<MovieDetail>,
    combinedFavorites: List<MovieDetail>,
    combinedWatchlist: List<MovieDetail>
) {
    WatchlistSection(
        title = "İzlenenler",
        items = combinedWatched,
        onTitleClick = { navController.navigate("full_list_screen/$profileUid/watched/İzlenenler") },
        onItemClick = { id, type -> navController.navigate(if (type == "series") "detailseries/$id" else "detailmovie/$id") }
    )
    Spacer(modifier = Modifier.height(16.dp))

    WatchlistSection(
        title = "Favoriler",
        items = combinedFavorites,
        onTitleClick = { navController.navigate("full_list_screen/$profileUid/favorites/Favoriler") },
        onItemClick = { id, type -> navController.navigate(if (type == "series") "detailseries/$id" else "detailmovie/$id") }
    )
    Spacer(modifier = Modifier.height(16.dp))

    WatchlistSection(
        title = "Daha Sonra İzle",
        items = combinedWatchlist,
        onTitleClick = { navController.navigate("full_list_screen/$profileUid/watchlist/Daha Sonra İzle") },
        onItemClick = { id, type -> navController.navigate(if (type == "series") "detailseries/$id" else "detailmovie/$id") }
    )
}