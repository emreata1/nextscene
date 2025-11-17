package com.example.nextscene.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    navController: NavController,
    targetUid: String,
    listType: String // "followers" veya "following"
) {
    val db = FirebaseFirestore.getInstance()
    var userList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val title = if (listType == "followers") "Takipçiler" else "Takip Edilenler"

    LaunchedEffect(targetUid, listType) {
        isLoading = true
        try {
            // 1. İlgili koleksiyondaki döküman ID'lerini (kullanıcı ID'leri) çek
            val collectionRef = db.collection("users").document(targetUid).collection(listType)
            val snapshot = collectionRef.get().await()
            val userIds = snapshot.documents.map { it.id }

            if (userIds.isNotEmpty()) {
                // 2. Bu ID'lere sahip kullanıcıların detaylarını 'users' koleksiyonundan çek
                // (Firestore'da 'in' sorgusu en fazla 10 eleman alır, bu yüzden tek tek veya chunk ile çekmek daha güvenli olabilir ama şimdilik basit tutalım)
                // En temiz yöntem: Her ID için belgeyi çekmek
                val users = mutableListOf<UserData>()
                for (uid in userIds) {
                    val userDoc = db.collection("users").document(uid).get().await()
                    val userData = userDoc.toObject(UserData::class.java)
                    if (userData != null) {
                        users.add(userData)
                    }
                }
                userList = users
            } else {
                userList = emptyList()
            }
        } catch (e: Exception) {
            // Hata yönetimi
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (userList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Kimse yok.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(userList) { user ->
                    // TimelineScreen'de kullandığımız UserSearchResultItem'ı burada tekrar kullanabiliriz!
                    UserSearchResultItem(
                        user = user,
                        onClick = {
                            // Listeden birine tıklayınca onun profiline git
                            navController.navigate("openProfile/${user.uid}")
                        }
                    )
                }
            }
        }
    }
}