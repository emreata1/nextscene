package com.example.nextscene.profile

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
import com.example.nextscene.auth.UserData
import com.example.nextscene.ui.UserSearchResultItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    navController: NavController,
    targetUid: String,
    listType: String
) {
    val db = FirebaseFirestore.getInstance()
    var userList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val title = if (listType == "followers") "Takipçiler" else "Takip Edilenler"

    LaunchedEffect(targetUid, listType) {
        isLoading = true
        try {
            val collectionRef = db.collection("users").document(targetUid).collection(listType)
            val snapshot = collectionRef.get().await()
            val userIds = snapshot.documents.map { it.id }

            if (userIds.isNotEmpty()) {

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
                    UserSearchResultItem(
                        user = user,
                        onClick = {
                            navController.navigate("openProfile/${user.uid}")
                        }
                    )
                }
            }
        }
    }
}