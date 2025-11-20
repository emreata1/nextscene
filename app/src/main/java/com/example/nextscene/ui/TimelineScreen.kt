package com.example.nextscene.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nextscene.auth.AuthViewModel
import com.example.nextscene.auth.UserData
import com.example.nextscene.profile.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
    val timestamp: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentCount: Int = 0
)

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var searchText by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostIdForComment by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchAllPosts()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.searchUsers(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kullanıcı Ara") },
                placeholder = { Text("@kullanıcı_adı ile ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchText.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Kullanıcı bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(searchResults) { user ->
                            UserSearchResultItem(
                                user = user,
                                onClick = { navController.navigate("openProfile/${user.uid}") }
                            )
                        }
                    }
                }
            } else {
                if (allPosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Henüz paylaşılan gönderi yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allPosts) { post ->
                            TimelinePostCard(
                                post = post,
                                currentUserId = currentUserId,
                                onLikeClick = { postId, isLiked ->
                                    val postRef = db.collection("posts").document(postId)
                                    if (isLiked) {
                                        postRef.update(
                                            "likedBy", FieldValue.arrayUnion(currentUserId),
                                            "likeCount", FieldValue.increment(1)
                                        )
                                    } else {
                                        postRef.update(
                                            "likedBy", FieldValue.arrayRemove(currentUserId),
                                            "likeCount", FieldValue.increment(-1)
                                        )
                                    }
                                },
                                onCommentClick = { postId ->
                                    selectedPostIdForComment = postId
                                    showCommentSheet = true
                                },
                                onPostClick = {
                                    navController.navigate("post_detail/${post.postId}")
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showCommentSheet) {
            CommentBottomSheet(
                postId = selectedPostIdForComment,
                currentUserId = currentUserId,
                onDismiss = { showCommentSheet = false }
            )
        }
    }
}


@Composable
fun UserSearchResultItem(user: UserData, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = { Text(user.username, fontWeight = FontWeight.Bold) },
        supportingContent = {
            if (user.name.isNotBlank()) Text("${user.name} ${user.surname}")
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profileImageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(user.profileImageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun TimelinePostCard(
    post: Post,
    currentUserId: String,
    onLikeClick: (String, Boolean) -> Unit,
    onCommentClick: (String) -> Unit,
    onPostClick: () -> Unit
) {

    var localIsLiked by remember { mutableStateOf(post.likedBy.contains(currentUserId)) }
    var localLikeCount by remember { mutableIntStateOf(post.likeCount) }

    LaunchedEffect(post) {
        localIsLiked = post.likedBy.contains(currentUserId)
        localLikeCount = post.likeCount
    }

    var username by remember { mutableStateOf("Yükleniyor...") }
    var userProfileImage by remember { mutableStateOf("") }

    LaunchedEffect(post.userId) {
        FirebaseFirestore.getInstance().collection("users").document(post.userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserData::class.java)
                if (user != null) {
                    username = user.username
                    userProfileImage = user.profileImageUrl ?: ""
                } else {
                    username = "Bilinmeyen Kullanıcı"
                }
            }
            .addOnFailureListener {
                username = "Hata"
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onPostClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfileImage.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(userProfileImage),
                            contentDescription = username,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = username.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "az önce", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                if (post.mediaPoster.isNotBlank() && post.mediaPoster != "N/A") {
                    Image(
                        painter = rememberAsyncImagePainter(post.mediaPoster),
                        contentDescription = null,
                        modifier = Modifier.width(90.dp).height(135.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.width(90.dp).height(135.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (post.title.isNotBlank()) {
                        Text(text = post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Text(text = post.mediaTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    Text(text = "${post.rating}/10", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
                    if (post.reviewText.isNotBlank()) {
                        Text(text = post.reviewText, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Row(
                    modifier = Modifier
                        .clickable {
                            val newLikedState = !localIsLiked
                            localIsLiked = newLikedState
                            localLikeCount = if (newLikedState) localLikeCount + 1 else localLikeCount - 1

                            onLikeClick(post.postId, newLikedState)
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (localIsLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Beğen",
                        tint = if (localIsLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (localLikeCount > 0) {
                        Text(text = "$localLikeCount", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Row(
                    modifier = Modifier
                        .clickable { onCommentClick(post.postId) }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Comment, contentDescription = "Yorum", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    if (post.commentCount > 0) {
                        Text(text = "${post.commentCount}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(postId: String, currentUserId: String, onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var text by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }

    LaunchedEffect(postId) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) comments = snapshot.toObjects(Comment::class.java)
            }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 400.dp, max = 600.dp)) {
            Text("Yorumlar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(comments) { comment ->
                    CommentItem(comment)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Yorum yaz...") })
                IconButton(onClick = {
                    if (text.isNotBlank()) {
                        val ref = db.collection("posts").document(postId).collection("comments").document()
                        ref.set(Comment(ref.id, postId, currentUserId, text = text))
                        db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1))
                        text = ""
                    }
                }) { Icon(Icons.AutoMirrored.Filled.Send, "Gönder") }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    var commentAuthorName by remember { mutableStateOf("...") }
    var commentAuthorImage by remember { mutableStateOf("") }

    LaunchedEffect(comment.userId) {
        FirebaseFirestore.getInstance().collection("users").document(comment.userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserData::class.java)
                if (user != null) {
                    commentAuthorName = user.username
                    commentAuthorImage = user.profileImageUrl ?: ""
                } else {
                    commentAuthorName = "Bilinmeyen"
                }
            }
    }

    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (commentAuthorImage.isNotBlank()) {
                Image(painter = rememberAsyncImagePainter(commentAuthorImage), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(commentAuthorName.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(commentAuthorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(comment.text, fontSize = 14.sp)
        }
    }
    HorizontalDivider()
}