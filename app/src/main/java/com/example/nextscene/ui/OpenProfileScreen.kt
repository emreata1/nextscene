package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.google.firebase.auth.FirebaseAuth
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
    val scope = rememberCoroutineScope()

    val profileUid = targetUid ?: currentUser?.uid

    // Veriler
    var displayUser by remember { mutableStateOf<UserData?>(null) }
    var realFollowerCount by remember { mutableLongStateOf(0) }
    var realFollowingCount by remember { mutableLongStateOf(0) }

    // --- LİSTELER (Film ve Dizi ayrımı burada, UI'da birleşecek) ---
    var watchedMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var favoriteMovies by remember { mutableStateOf(listOf<MovieDetail>()) }
    var watchlistMovies by remember { mutableStateOf(listOf<MovieDetail>()) } // YENİ: Daha Sonra İzle (Film)

    var watchedSeries by remember { mutableStateOf(listOf<MovieDetail>()) }
    var favoriteSeries by remember { mutableStateOf(listOf<MovieDetail>()) }
    var watchlistSeries by remember { mutableStateOf(listOf<MovieDetail>()) } // YENİ: Daha Sonra İzle (Dizi)

    var isLoading by remember { mutableStateOf(true) }

    // Takip Durumu
    var isFollowing by remember { mutableStateOf(false) }
    var followLoading by remember { mutableStateOf(false) }

    LaunchedEffect(profileUid) {
        if (profileUid != null) {
            isLoading = true
            try {
                // 1. Kullanıcı Bilgileri
                val userDoc = db.collection("users").document(profileUid).get().await()
                displayUser = userDoc.toObject(UserData::class.java)
                val followerTask = async { authViewModel.getFollowerCountFromSubcollection(profileUid) }
                val followingTask = async { authViewModel.getFollowingCountFromSubcollection(profileUid) }

                realFollowerCount = followerTask.await()
                realFollowingCount = followingTask.await()

                if (currentUser != null && profileUid != currentUser?.uid) {
                    isFollowing = authViewModel.isUserFollowing(profileUid)
                }

                // 2. ID'leri Çek (Not: 'watchlistMovies' ve 'watchlistSeries' koleksiyon isimlerini Firebase'e göre ayarla)
                val userRef = db.collection("users").document(profileUid)

                val wM_Ids = userRef.collection("watchedMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val fM_Ids = userRef.collection("favoriteMovies").get().await().documents.mapNotNull { it.getString("movieId") }
                val wlM_Ids = userRef.collection("watchlistMovies").get().await().documents.mapNotNull { it.getString("movieId") } // YENİ

                val wS_Ids = userRef.collection("watchedSeries").get().await().documents.mapNotNull { it.getString("seriesId") }
                val fS_Ids = userRef.collection("favoriteSeries").get().await().documents.mapNotNull { it.getString("seriesId") }
                val wlS_Ids = userRef.collection("watchlistSeries").get().await().documents.mapNotNull { it.getString("seriesId") } // YENİ

                val apiKey = "b9bd48a6"

                // 3. API'den Detayları Çek
                // Filmler
                watchedMovies = wM_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                favoriteMovies = fM_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                watchlistMovies = wlM_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()

                // Diziler
                watchedSeries = wS_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                favoriteSeries = fS_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()
                watchlistSeries = wlS_Ids.map { id -> async { NetworkModule.omdbApiService.getMovieDetail(imdbID = id, apiKey = apiKey) } }.awaitAll().filterNotNull()

            } catch (e: Exception) {
                println("Hata: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold { padding ->
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
                // Listeleri Birleştiriyoruz (UI için)
                val combinedWatched = (watchedMovies + watchedSeries).shuffled() // Karışık gözüksün diye shuffle
                val combinedFavorites = (favoriteMovies + favoriteSeries).shuffled()
                val combinedWatchlist = (watchlistMovies + watchlistSeries).shuffled()

                val totalWatchedCount = combinedWatched.size

                // Profile Header
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

                // --- TAKİP BUTONU ---
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

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(24.dp))

                // --- 1. BÖLÜM: İZLENENLER (Watched) ---
                WatchlistSection(
                    title = "İzlenenler",
                    items = combinedWatched,
                    onTitleClick = {
                        // ListType: "watched" olarak gönderiyoruz
                        navController.navigate("full_list_screen/$profileUid/watched/İzlenenler")
                    },
                    onItemClick = { id, type ->
                        val route = if (type == "series") "detailseries/$id" else "detailmovie/$id"
                        navController.navigate(route)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. BÖLÜM: FAVORİLER (Favorites) ---
                WatchlistSection(
                    title = "Favoriler",
                    items = combinedFavorites,
                    onTitleClick = {
                        navController.navigate("full_list_screen/$profileUid/favorites/Favoriler")
                    },
                    onItemClick = { id, type ->
                        val route = if (type == "series") "detailseries/$id" else "detailmovie/$id"
                        navController.navigate(route)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. BÖLÜM: DAHA SONRA İZLE (Watchlist) ---
                WatchlistSection(
                    title = "Daha Sonra İzle",
                    items = combinedWatchlist,
                    onTitleClick = {
                        navController.navigate("full_list_screen/$profileUid/watchlist/Daha Sonra İzle")
                    },
                    onItemClick = { id, type ->
                        val route = if (type == "series") "detailseries/$id" else "detailmovie/$id"
                        navController.navigate(route)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp)) // En altta biraz boşluk
            }
        }
    }
}

// --- ProfileHeader ve WatchlistSection KODLARI AYNI KALABİLİR ---
// (Yukarıdaki WatchlistSection'da LazyRow düzeltmesi zaten yapılmıştı,
// onu burada tekrar yazmaya gerek yok, önceki cevaptaki gibi kalabilir)

@Composable
fun ProfileHeader(
    userData: UserData?,
    watchedCount: Int,
    followerCount: Int,
    followingCount: Int,
    navController: NavController,
    onFollowClick: (String) -> Unit
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

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
            // --- Sol taraf (profil resmi, isim, bio) ---
            Column(horizontalAlignment = Alignment.Start) {
                // Profil resmi
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

                // İsim
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

            // --- Sağ taraf (kullanıcı adı + ayarlar + sayaçlar) ---
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {

                // Kullanıcı adı + Settings aynı satırda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = userData?.username ?: "Kullanıcı Adı",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Sadece kendi profiliyse göster
                    if (userData?.uid == currentUid) {
                        IconButton(
                            onClick = {
                                navController.navigate("settings") // Route adını kontrol et
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sayaçlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // İzlenenler (Sadece sayı)
                    StatItem(count = watchedCount, label = "İzlediği")

                    // Takipçi Sayısı
                    StatItem(
                        count = followerCount,
                        label = "Takipçi",
                        onClick = { onFollowClick("followers") }
                    )

                    // Takip Sayısı
                    StatItem(
                        count = followingCount,
                        label = "Takip",
                        onClick = { onFollowClick("following") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp)
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
fun WatchlistSection(
    title: String,
    items: List<MovieDetail>,
    onTitleClick: () -> Unit, // Başlığa tıklama olayı
    onItemClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Başlık Satırı (Tıklanabilir)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTitleClick() } // Tıklanınca tümünü gör
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // "Tümünü Gör" hissiyatı veren bir ikon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward, // Veya Icons.Default.ArrowForward
                contentDescription = "Tümünü Gör",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "Henüz $title listenizde bir şey yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Profilde sadece ilk 10 öğeyi gösterip performansı koruyabiliriz
            val displayItems = items.take(10)

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(displayItems) { item ->
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