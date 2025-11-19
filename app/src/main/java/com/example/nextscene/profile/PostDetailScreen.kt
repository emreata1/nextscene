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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.auth.UserData
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.network.NetworkModule
import com.example.nextscene.profile.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavController
) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var post by remember { mutableStateOf<Post?>(null) }
    var postOwner by remember { mutableStateOf<UserData?>(null) }
    var mediaDetail by remember { mutableStateOf<MovieDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(postId) {
        isLoading = true
        errorMessage = null
        try {
            // 1. Gönderi verisini çek
            val postDoc = db.collection("posts").document(postId).get().await()
            val fetchedPost = postDoc.toObject(Post::class.java)

            if (fetchedPost != null) {
                post = fetchedPost

                // 2. Medya Detaylarını Çek
                val media = NetworkModule.omdbApiService.getMovieDetail(
                    imdbID = fetchedPost.mediaId,
                    apiKey = "b9bd48a6"
                )
                mediaDetail = media

                // 3. Kullanıcı Verisini Çek
                val userDoc = db.collection("users").document(fetchedPost.userId).get().await()
                postOwner = userDoc.toObject(UserData::class.java)

            } else {
                errorMessage = "Gönderi bulunamadı."
            }
        } catch (e: Exception) {
            errorMessage = "Veri çekilirken hata oluştu: ${e.message}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gönderiler") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (post != null && postOwner != null) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // --- 1. GÖNDERİ SAHİBİ (Üst Kısım) ---
                PostOwnerHeader(postOwner!!, navController)

                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. GÖNDERİ BAŞLIĞI ve YORUM ---
                Text(
                    text = post!!.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = post!!.reviewText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. MEDYA VE PUAN BİLGİSİ ---
                mediaDetail?.let { media ->
                    MediaDetailCard(post!!, media)
                } ?: run {
                    Text("Medya detayları yüklenemedi.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// --- YARDIMCI COMPOSABLE'LAR ---
// -----------------------------------------------------------------------------------------

@Composable
fun PostOwnerHeader(userData: UserData, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("openProfile/${userData.uid}") }
            ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profil Resmi
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = userData.profileImageUrl
            if (imageUrl?.isNotBlank() == true) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = "Profil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Kullanıcı Adı ve İsim
        Column {
            Text(
                text = userData.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val fullName = if (userData.name.isNotBlank() && userData.surname.isNotBlank()) "${userData.name} ${userData.surname}" else null
            fullName?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MediaDetailCard(post: Post, media: MovieDetail) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Medya Posteri
            Image(
                painter = rememberAsyncImagePainter(media.Poster),
                contentDescription = media.Title,
                modifier = Modifier
                    .size(120.dp, 180.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Detaylar ve Puan
            Column(modifier = Modifier.weight(1f)) {
                // Film/Dizi Başlığı
                media.Title?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tür ve Yıl
                val mediaType = media.Type?.replaceFirstChar { it.uppercase() } ?: "İçerik"
                val releaseInfo = if (media.Year.isNullOrBlank()) mediaType else "${mediaType}, ${media.Year}"
                Text(releaseInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Kullanıcının Puanı
                Text(
                    text = "VERDİĞİNİZ PUAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${post.rating}/10",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // IMDb Puanı (Ek bilgi)
                media.imdbRating?.let { imdbRating ->
                    Text(
                        text = "IMDb Puanı: $imdbRating",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Medyanın Tam Özetini alt kısma ekleyebiliriz
        media.Plot?.let { plot ->
            if (plot != "N/A" && plot.isNotBlank()) {
                Text(
                    text = "Özet: $plot",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp).padding(top = 0.dp)
                )
            }
        }
    }
}