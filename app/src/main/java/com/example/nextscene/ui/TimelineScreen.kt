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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    navController: NavController, // Sonuçlara tıklamak için NavController gerekli
    viewModel: AuthViewModel = viewModel() // ViewModel'i al
) {

    // 1. Arama metni state'i
    var searchText by remember { mutableStateOf("") }

    // 2. ViewModel'den arama sonuçlarını dinle
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3. Arama Barı
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

        // 4. Arama Sonuçlarını Gösteren Liste

        // Arama çubuğu boşsa, normal akışı göster (veya bir mesaj)
        if (searchText.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kullanıcıları aramak için yazmaya başlayın.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Arama çubuğu dolu ama sonuç yoksa
        else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kullanıcı bulunamadı.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Sonuç bulunduysa
        else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Kalan tüm alanı kapla
            ) {
                items(searchResults) { user ->
                    UserSearchResultItem(
                        user = user,
                        onClick = {
                            // Tıklayınca o kullanıcının profiline git
                            // (Screen.kt'deki OpenProfile rotasını kullanıyoruz)
                            navController.navigate("openProfile/${user.uid}")
                        }
                    )
                }
            }
        }
    }
}

// Arama listesindeki her bir satır için composable

@Composable
fun UserSearchResultItem(
    user: UserData,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        // --- HATA BURADAYDI ---
        headlineContent = { // 'headlineText' yerine 'headlineContent'
            Text(user.username, fontWeight = FontWeight.Bold)
        },
        // --- VE BURADAYDI ---
        supportingContent = { // 'supportingText' yerine 'supportingContent'
            // İsim/Soyisim varsa onu da göster
            if (user.name.isNotBlank() && user.surname.isNotBlank()) {
                Text("${user.name} ${user.surname}")
            }
        },
        // --- BU KISIM DOĞRUYDU ---
        leadingContent = {
            // OpenProfileScreen'deki profil resmi mantığının aynısı
            Box(
                modifier = Modifier
                    .size(40.dp) // Liste için daha küçük
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = user.profileImageUrl
                if (imageUrl.isNotBlank()) {
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