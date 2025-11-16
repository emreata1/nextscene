package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.network.NetworkModule
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    targetUid: String? = null
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser by authViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope() // Buton işlemleri için scope

    // Bu profil kime ait?
    val profileUid = targetUid ?: currentUser?.uid

    // Veriler
    var displayUser by remember { mutableStateOf<UserData?>(null) }
    var realFollowerCount by remember { mutableLongStateOf(0) }
    var realFollowingCount by remember { mutableLongStateOf(0) }
    var watchedMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var favoriteMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var watchedSeries by remember { mutableStateOf(listOf<MovieDetail>()) }
    var favoriteSeries by remember { mutableStateOf(listOf<MovieDetail>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Takip Durumu
    var isFollowing by remember { mutableStateOf(false) }
    var followLoading by remember { mutableStateOf(false) } // Buton loading durumu

    // Verileri Çekme Mantığı
    LaunchedEffect(profileUid) {
        if (profileUid != null) {
            isLoading = true
            try {
                // 1. Kullanıcıyı Çek
                val userDoc = db.collection("users").document(profileUid).get().await()
                displayUser = userDoc.toObject(UserData::class.java)
                val followerTask = async { authViewModel.getFollowerCountFromSubcollection(profileUid) }
                val followingTask = async { authViewModel.getFollowingCountFromSubcollection(profileUid) }

                realFollowerCount = followerTask.await()
                realFollowingCount = followingTask.await()

                // 2. Takip Ediyor muyuz? (Başkasının profiliyse)
                if (currentUser != null && profileUid != currentUser?.uid) {
                    isFollowing = authViewModel.isUserFollowing(profileUid)
                }

                // 3. Listeleri Çek
                val userRef = db.collection("users").document(profileUid)
                val wM_Ids = userRef.collection("watchedMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val fM_Ids = userRef.collection("favoriteMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val wS_Ids = userRef.collection("watchedSeries").get().await().documents.mapNotNull { it.getString("seriesId") }
                val fS_Ids = userRef.collection("favoriteSeries").get().await().documents.mapNotNull { it.getString("seriesId") }

                // 4. API Detayları
                val apiKey = "b9bd48a6" // API KEY'İ BURAYA
                // 3. API'den Detayları Çek (DÜZELTİLMİŞ KOD)

                watchedMovies = wM_Ids.map { id ->
                    async {
                        // Parametre ismini (apiKey) belirterek ve string'i direkt vererek çağırıyoruz
                        NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = "b9bd48a6")
                    }
                }.awaitAll().filterNotNull()

                favoriteMovies = fM_Ids.map { id ->
                    async {
                        NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = "b9bd48a6")
                    }
                }.awaitAll().filterNotNull()

                watchedSeries = wS_Ids.map { id ->
                    async {
                        NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = "b9bd48a6")
                    }
                }.awaitAll().filterNotNull()

                favoriteSeries = fS_Ids.map { id ->
                    async {
                        NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = "b9bd48a6")
                    }
                }.awaitAll().filterNotNull()
            } catch (e: Exception) {
                println("Hata: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            if (profileUid == currentUser?.uid) {
                TopAppBar(
                    title = { Text("") },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                        }
                    }
                )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val watchedCount = watchedMovies.size + watchedSeries.size

                // Profile Header (Sayaçlar burada güncelleniyor)
                ProfileHeader(
                    userData = displayUser,
                    watchedCount = watchedCount,
                    followerCount = realFollowerCount.toInt(), // Long -> Int çevirimi
                    followingCount = realFollowingCount.toInt() // Long -> Int çevirimi
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- TAKİP ET / TAKİPTEN ÇIK BUTONU ---
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
                        if (followLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(text = if (isFollowing) "Takipten Çık" else "Takip Et")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                // --------------------------------------

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(24.dp))

                WatchlistSection(title = "Favori Filmler", items = favoriteMovies, onItemClick = { id, _ -> navController.navigate("detailmovie/$id") })
                Spacer(modifier = Modifier.height(16.dp))
                WatchlistSection(title = "İzlenen Filmler", items = watchedMovies, onItemClick = { id, _ -> navController.navigate("detailmovie/$id") })
                Spacer(modifier = Modifier.height(16.dp))
                WatchlistSection(title = "Favori Diziler", items = favoriteSeries, onItemClick = { id, _ -> navController.navigate("detailseries/$id") })
                Spacer(modifier = Modifier.height(16.dp))
                WatchlistSection(title = "İzlenen Diziler", items = watchedSeries, onItemClick = { id, _ -> navController.navigate("detailseries/$id") })
            }
        }
    }
}

// --- ProfileHeader ve WatchlistSection AYNI KALIYOR (Senin son attığın kodla aynı) ---
@Composable
fun ProfileHeader(
    userData: UserData?,
    watchedCount: Int,
    followerCount: Int,
    followingCount: Int
) {
    val displayName = remember(userData) {
        if (userData != null && userData.name.isNotBlank() && userData.surname.isNotBlank()) {
            "${userData.name} ${userData.surname}"
        } else if (userData != null && userData.username.isNotBlank()) {
            userData.username
        } else {
            "Kullanıcı"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sol Taraf: Profil Resmi ve Bio
            Column(horizontalAlignment = Alignment.Start) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (userData?.profileImageUrl.isNullOrEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profil Resmi",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(userData?.profileImageUrl),
                            contentDescription = "Profil Resmi",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // İsim (Sol tarafa eklendi senin son tasarımındaki gibi)
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis ,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )

                // Bio
                if (userData?.bio?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userData.bio,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.width(96.dp).padding(start = 8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sağ Taraf: Kullanıcı Adı ve Sayaçlar
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = userData?.username ?: "Kullanıcı Adı",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(count = watchedCount, label = "İzlediği")
                    StatItem(count = followerCount, label = "Takipçi")
                    StatItem(count = followingCount, label = "Takip")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WatchlistSection(title: String, items: List<MovieDetail>, onItemClick: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (items.isEmpty()) {
            Text(
                text = "Henüz $title listenizde bir şey yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clickable {
                                onItemClick(item.imdbID, item.Type)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(item.Poster),
                            contentDescription = item.Title,
                            modifier = Modifier
                                .size(120.dp, 180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.Title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}