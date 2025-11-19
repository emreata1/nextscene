package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.auth.AuthViewModel
import com.example.nextscene.auth.UserData

// PostItem ve Posts/UserData sınıflarının import'u (Paket yolunuzu kontrol edin!)
import com.example.nextscene.profile.PostItem

// import com.example.nextscene.ui.Post
// import com.example.nextscene.auth.UserData


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {

    // 1. Arama metni state'i
    var searchText by remember { mutableStateOf("") }

    // 2. ViewModel'den arama sonuçlarını dinle (List<UserData>)
    val searchResults by viewModel.searchResults.collectAsState()

    // 3. ViewModel'den tüm gönderileri dinle (List<Post>)
    val allPosts by viewModel.allPosts.collectAsState()


    // Sayfa yüklendiğinde gönderileri çekmek için LaunchedEffect
    LaunchedEffect(Unit) {
        // ViewModel'deki tüm gönderileri çekme fonksiyonunu çağır
        viewModel.fetchAllPosts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 4. Arama Barı
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                // Metin değiştikçe ViewModel'deki arama fonksiyonunu tetikle
                viewModel.searchUsers(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kullanıcı Ara") },
            placeholder = { Text("@kullanıcı_adı ile ara...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ara"
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- İÇERİK BÖLÜMÜ (Arama veya Zaman Tüneli) ---

        // Arama çubuğu doluysa (Kullanıcı arama yapıyor)
        if (searchText.isNotBlank()) {

            // Sonuç yoksa
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kullanıcı bulunamadı.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Sonuç bulunduysa (Kullanıcı Listesi)
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // DÜZELTME: Kullanıcı sonuçları listelenir ve UserSearchResultItem kullanılır.
                    items(searchResults) { user ->
                        UserSearchResultItem(
                            user = user,
                            onClick = {
                                // Tıklanınca o kullanıcının profiline git
                                navController.navigate("openProfile/${user.uid}")
                            }
                        )
                    }
                }
            }
        }
        // Arama çubuğu boşsa (Zaman Tüneli / Tüm Gönderiler)
        else {
            if (allPosts.isEmpty()) { // Veya isPostsLoading kontrolü
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz paylaşılan gönderi yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Gönderiler arasına boşluk
                ) {
                    // Gönderileri en yeniden eskiye doğru listeler
                    items(allPosts) { post ->
                        PostItem(
                            post = post,
                            onClick = { postId ->
                                // DÜZELTME: Tıklanınca genişletilmiş görünüme (detay rotasına) git
                                navController.navigate("post_detail/$postId")
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- YARDIMCI BİLEŞEN: KULLANICI ARAMA SONUCU ---

@Composable
fun UserSearchResultItem(
    user: UserData,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        headlineContent = {
            Text(user.username, fontWeight = FontWeight.Bold)
        },
        supportingContent = {
            if (user.name.isNotBlank() && user.surname.isNotBlank()) {
                Text("${user.name} ${user.surname}")
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = user.profileImageUrl
                if (imageUrl?.isNotBlank() == true) { // Null check eklendi
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = user.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}