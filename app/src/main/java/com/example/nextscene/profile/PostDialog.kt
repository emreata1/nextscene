package com.example.nextscene.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.data.Movie
import com.example.nextscene.data.MovieDetail
import com.example.nextscene.network.NetworkModule
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color

// (Post, Movie, MovieDetail, NetworkModule vb. sınıfların import edildiği varsayılır)

// Yardımcı Tür Sınıfı
data class SearchType(val display: String, val omdbValue: String?)

@Composable
fun NewPostDialog(
    currentUserId: String,
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    // --- Arama Durumları ---
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var searchResults by remember { mutableStateOf<List<Movie>?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // --- YENİ: Tür Seçimi Durumları ---
    val searchTypes = remember {
        listOf(
            SearchType("Hepsi", null),
            SearchType("Film", "movie"),
            SearchType("Dizi", "series")
        )
    }
    var selectedSearchType by remember { mutableStateOf(searchTypes[0]) } // Varsayılan: Hepsi

    // --- Gönderi Durumları ---
    var postTitle by remember { mutableStateOf(TextFieldValue("")) }
    var reviewText by remember { mutableStateOf(TextFieldValue("")) }
    var rating by remember { mutableIntStateOf(5) }
    var selectedMediaDetail by remember { mutableStateOf<MovieDetail?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    // OMDB API Arama Fonksiyonu (GÜNCELLENDİ)
    val performSearch: (String, String?) -> Unit = { query, type ->
        if (query.length >= 3) {
            isSearching = true
            scope.launch {
                try {
                    // OMDB API'sine type parametresi eklendi
                    val response = NetworkModule.omdbApiService.searchMoviesWithType(
                        query = query,
                        apiKey = "b9bd48a6",
                        type = type
                    )
                    searchResults = response.Search ?: emptyList()
                } catch (e: Exception) {
                    println("Arama Hatası: ${e.message}")
                    searchResults = emptyList()
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchResults = null
        }
    }

    // NOT: NetworkModule.omdbApiService interface'inize bu fonksiyonu eklemeyi unutmayın!
    /*
    interface OmdbApiService {
        // ...
        @GET("/")
        suspend fun searchMoviesWithType(
            @Query("s") query: String,
            @Query("apikey") apiKey: String,
            @Query("type") type: String? // Burası null/boş olursa tümünü getirir
        ): SearchResponse
    }
    */


    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 650.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Yeni Gönderi Oluştur", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // --- 1. SEÇİM BÖLÜMÜ: ARAMA ve Tür Seçimi ---
                if (selectedMediaDetail == null) {
                    // Tür Seçimi UI'ı (İSTEĞE BAĞLI: Dropdown yerine SegmentedButton da kullanılabilir)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("İçerik Türü:", style = MaterialTheme.typography.bodyLarge)

                        var expanded by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text(selectedSearchType.display)
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                searchTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.display) },
                                        onClick = {
                                            selectedSearchType = type
                                            expanded = false
                                            // Tür değiştiğinde aramayı tetikle (eğer 3 karakterden uzunsa)
                                            performSearch(searchQuery.text, selectedSearchType.omdbValue)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Arama Kutusu
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            // Arama metni değiştiğinde seçili türe göre aramayı tetikle
                            performSearch(it.text, selectedSearchType.omdbValue)
                        },
                        label = { Text("Başlık Ara") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Arama Sonuçları Listesi
                    val results = searchResults
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                        if (results != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(results) { movie ->
                                    MovieSearchItem(movie = movie, onSelect = { selectedMovie ->
                                        isSearching = true
                                        scope.launch {
                                            try {
                                                val detail = NetworkModule.omdbApiService.getMovieDetail(
                                                    imdbID = selectedMovie.imdbID,
                                                    apiKey = "b9bd48a6"
                                                )
                                                if (detail?.Response == "False") {
                                                    println("Hata: Film Detayı Bulunamadı.")
                                                    searchResults = null
                                                } else {
                                                    selectedMediaDetail = detail
                                                    searchResults = null
                                                    searchQuery = TextFieldValue("")
                                                }
                                            } catch (e: Exception) {
                                                println("Detay Çekme Hatası: ${e.message}")
                                            } finally {
                                                isSearching = false
                                            }
                                        }
                                    })
                                }
                            }
                        } else if (!isSearching && searchQuery.text.length >= 3) {
                            Text("Sonuç bulunamadı.", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    if (results != null || (!isSearching && searchQuery.text.length >= 3)) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Seçilen Film/Dizi Bilgisi
                selectedMediaDetail?.let {
                    SelectedMediaCard(it, onRemove = { selectedMediaDetail = null })
                    Spacer(modifier = Modifier.height(16.dp))
                }


                // --- 2. GÖNDERİ FORMU BÖLÜMÜ (Koşulsuz Görünür) ---

                // PUANLAMA VE BAŞLIK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Puanınız: ${rating}/10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    // Puanlama Görseli
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(rating) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp))
                        }
                        repeat(10 - rating) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Puanlama (Slider)
                Slider(
                    value = rating.toFloat(),
                    onValueChange = { rating = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))


                // Gönderi Başlığı
                OutlinedTextField(
                    value = postTitle,
                    onValueChange = { postTitle = it },
                    label = { Text("Gönderi Başlığı") },
                    placeholder = { Text("İnsanları çekecek bir başlık...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Açıklama/Yorum
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Yorumunuz/Açıklamanız") },
                    placeholder = { Text("İçerik hakkındaki düşüncelerinizi yazın...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Gönder Butonu
                Button(
                    onClick = {
                        selectedMediaDetail?.let { media ->
                            isPosting = true
                            scope.launch {
                                val postId = db.collection("posts").document().id
                                val newPost = Post(
                                    postId = postId,
                                    userId = currentUserId,
                                    mediaId = media.imdbID,
                                    mediaTitle = media.Title ?: "Bilinmeyen Başlık",
                                    mediaPoster = media.Poster ?: "",
                                    mediaType = media.Type?.lowercase() ?: "bilinmeyen",
                                    title = postTitle.text,
                                    reviewText = reviewText.text,
                                    rating = rating
                                )
                                db.collection("posts").document(postId).set(newPost).await()
                                isPosting = false
                                onPostCreated()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    // Koşul: Medya seçili OLMALI ve Başlık boş OLMAMALI
                    enabled = selectedMediaDetail != null && postTitle.text.isNotBlank() && !isPosting
                ) {
                    if (isPosting) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } else { Text("Paylaş") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !isPosting) {
                    Text("İptal")
                }
            }
        }
    }
}

// --- YARDIMCI COMPOSABLE'LAR (Aynı Kaldı) ---

@Composable
fun MovieSearchItem(movie: Movie, onSelect: (Movie) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect(movie) }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val posterUrl = movie.Poster.takeIf { !it.isNullOrEmpty() && it != "N/A" }

        Card(modifier = Modifier.size(40.dp, 60.dp), shape = RoundedCornerShape(4.dp)) {
            Image(
                painter = rememberAsyncImagePainter(model = posterUrl),
                contentDescription = movie.Title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val titleText = movie.Title ?: "Başlık Yok"
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val yearText = movie.Year ?: "Bilinmiyor"
            val typeText = movie.Type?.replaceFirstChar { it.uppercase() } ?: "İçerik"

            Text(
                text = "${yearText} (${typeText})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SelectedMediaCard(mediaDetail: MovieDetail, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Poster
                Card(modifier = Modifier.size(60.dp, 90.dp), shape = RoundedCornerShape(6.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(model = mediaDetail.Poster.takeIf { it != "N/A" }),
                        contentDescription = mediaDetail.Title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Başlık
                Column {
                    mediaDetail.Title?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${mediaDetail.Year} (${mediaDetail.Type})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Kaldır Butonu
            IconButton(onClick = onRemove, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                Icon(Icons.Default.Close, contentDescription = "Seçimi Kaldır")
            }
        }
    }
}