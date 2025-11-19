package com.example.nextscene.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.auth.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )

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

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {

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

                    if (userData?.uid == currentUid) {
                        IconButton(
                            onClick = {
                                navController.navigate("settings")
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(count = watchedCount, label = "İzlediği")

                    StatItem(
                        count = followerCount,
                        label = "Takipçi",
                        onClick = { onFollowClick("followers") }
                    )

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
    onTitleClick: () -> Unit,
    onItemClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTitleClick() }
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
                                item.Type?.let { onItemClick(item.imdbID, it) }
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
                        item.Title?.let {
                            Text(
                                text = it,
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
}

@Composable
fun PostsSection(
    posts: List<Post>,
    onPostClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (posts.isEmpty()) {
            Text(
                text = "Henüz bir gönderi yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            posts.take(3).forEach { post ->
                PostItem(post = post, onClick = onPostClick)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (posts.size > 3) {
                TextButton(onClick = { /* Tümünü gör mantığı */ }) {
                    Text("Tüm Gönderileri Gör (${posts.size} adet)")
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, onClick: (String) -> Unit) {
    // 1. DÜZELTME: Firebase Firestore Instance'ını tanımla
    val db = FirebaseFirestore.getInstance()

    // YENİ DURUM: Gönderi sahibinin kullanıcı adını tutar
    var postOwnerUsername by remember { mutableStateOf<String?>(null) }

    // DÜZELTME: postOwnerUsername'in ilk gösterim değeri, veri çekilene kadar UID'den gelir
    val displayName = postOwnerUsername ?: "${post.userId.take(4)}..."


    // YENİ EFFECT: post.userId değiştiğinde kullanıcı adını çeker
    LaunchedEffect(post.userId) {
        try {
            val userDoc = db.collection("users").document(post.userId).get().await()
            // UserData sınıfınızda 'username' alanı olduğunu varsayıyoruz
            postOwnerUsername = userDoc.getString("username")
        } catch (e: Exception) {
            println("Kullanıcı Adı Çekme Hatası: ${e.message}")
            postOwnerUsername = "Kullanıcı Bilinmiyor"
        }
    }

    // Zaman damgasını okunabilir hale getirme
    val formattedTime = remember(post.timestamp) {
        val now = System.currentTimeMillis()
        val diff = now - post.timestamp
        when {
            diff < 60000 -> "Şimdi"
            diff < 3600000 -> "${diff / 60000} dakika önce"
            diff < 86400000 -> "${diff / 3600000} saat önce"
            else -> "${diff / 86400000} gün önce"
        }
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(post.postId) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profil",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        // BURASI GÜNCELLENDİ
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = rememberAsyncImagePainter(post.mediaPoster),
                    contentDescription = post.mediaTitle,
                    modifier = Modifier
                        .size(90.dp, 135.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = post.mediaTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${post.rating}/10",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.reviewText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


