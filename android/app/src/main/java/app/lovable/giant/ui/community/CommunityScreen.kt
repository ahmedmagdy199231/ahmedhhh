package app.lovable.giant.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lovable.giant.data.models.CommunityCommentModel
import app.lovable.giant.data.models.CommunityPostModel
import app.lovable.giant.ui.stories.CreateStoryDialog
import app.lovable.giant.ui.stories.StoriesBar
import app.lovable.giant.ui.stories.StoriesViewModel
import app.lovable.giant.ui.stories.StoryViewerDialog
import coil.compose.AsyncImage

private val REACTIONS_LIST = listOf(
    Pair("like", "👍"),
    Pair("love", "❤️"),
    Pair("haha", "😂"),
    Pair("wow", "😮"),
    Pair("sad", "😢"),
    Pair("angry", "😡")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateBack: () -> Unit,
    viewModel: CommunityViewModel = viewModel(),
    storiesViewModel: StoriesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val storiesState by storiesViewModel.uiState.collectAsState()

    var showComposer by remember { mutableStateOf(false) }
    var composerContent by remember { mutableStateOf("") }
    var composerMediaUrl by remember { mutableStateOf("") }
    var reportDialogPostId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المجتمع والقصص",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.posts.size} منشور · ${storiesState.activeStoryUsers.size} قصة نشطة",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadPosts()
                        storiesViewModel.loadActiveStories()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = Color(0xFF059669),
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "منشور جديد")
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            // 1. Stories Bar
            item {
                StoriesBar(
                    stories = storiesState.activeStoryUsers,
                    myUserId = uiState.currentUserId,
                    onUserClick = { storiesViewModel.openStoryViewer(it) },
                    onCreateClick = { storiesViewModel.openCreateStory() }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 2. Search and Filter Chips
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث في المنشورات...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF059669),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.sortKey == "newest",
                            onClick = { viewModel.setSort("newest") },
                            label = { Text("الأحدث", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.sortKey == "trending",
                            onClick = { viewModel.setSort("trending") },
                            label = { Text("الأكثر تفاعلاً", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.mediaFilter == "all",
                            onClick = { viewModel.setMediaFilter("all") },
                            label = { Text("الكل", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.mediaFilter == "image",
                            onClick = { viewModel.setMediaFilter("image") },
                            label = { Text("صور", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.showOnlyMine,
                            onClick = { viewModel.toggleShowOnlyMine() },
                            label = { Text("منشوراتي", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // 3. Posts List
            val filteredPosts = uiState.posts.filter { post ->
                val matchesSearch = uiState.searchQuery.isBlank() ||
                        (post.content?.contains(uiState.searchQuery, ignoreCase = true) == true) ||
                        (post.authorUsername?.contains(uiState.searchQuery, ignoreCase = true) == true)

                val matchesMedia = when (uiState.mediaFilter) {
                    "image" -> post.mediaType?.startsWith("image") == true
                    "video" -> post.mediaType?.startsWith("video") == true
                    "text" -> post.mediaUrl.isNullOrEmpty()
                    else -> true
                }

                val matchesMine = !uiState.showOnlyMine || post.authorId == uiState.currentUserId

                matchesSearch && matchesMedia && matchesMine
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF059669))
                    }
                }
            } else if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد منشورات حالياً — شارك أول منشور!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        isOwner = post.authorId == uiState.currentUserId,
                        onReact = { rx -> viewModel.reactToPost(post.id, rx) },
                        onOpenComments = { viewModel.openComments(post.id) },
                        onToggleSave = { viewModel.toggleSavePost(post.id) },
                        onDelete = { viewModel.deletePost(post.id) },
                        onReport = { reportDialogPostId = post.id },
                        isSaved = uiState.savedPostIds.contains(post.id)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Create Post Dialog
        if (showComposer) {
            AlertDialog(
                onDismissRequest = { showComposer = false },
                title = { Text("إنشاء منشور جديد ✨", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = composerContent,
                            onValueChange = { composerContent = it },
                            placeholder = { Text("ماذا يدور في ذهنك؟ #هاشتاق @منشن", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = composerMediaUrl,
                            onValueChange = { composerMediaUrl = it },
                            placeholder = { Text("رابط صورة أو فيديو (اختياري)", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (composerContent.isNotBlank() || composerMediaUrl.isNotBlank()) {
                                val mType = if (composerMediaUrl.endsWith(".mp4")) "video" else if (composerMediaUrl.isNotBlank()) "image" else null
                                val kind = if (composerMediaUrl.isNotBlank() && composerContent.isNotBlank()) "mixed" else if (composerMediaUrl.isNotBlank()) "image" else "text"
                                viewModel.createPost(composerContent, composerMediaUrl.ifBlank { null }, mType, kind)
                                composerContent = ""
                                composerMediaUrl = ""
                                showComposer = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("نشر", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showComposer = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Report Post Dialog
        if (reportDialogPostId != null) {
            AlertDialog(
                onDismissRequest = { reportDialogPostId = null },
                title = { Text("إبلاغ عن المنشور", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("سبب الإبلاغ (محتوى غير لائق...)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            reportDialogPostId?.let { pId ->
                                viewModel.reportPost(pId, reportReason.ifBlank { "محتوى غير لائق" })
                            }
                            reportDialogPostId = null
                            reportReason = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("إرسال البلاغ", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { reportDialogPostId = null }) {
                        Text("إلغاء", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Comments BottomSheet
        if (uiState.commentsPostId != null) {
            CommentsBottomSheet(
                postId = uiState.commentsPostId!!,
                comments = uiState.comments,
                isLoading = uiState.isCommentsLoading,
                currentUserId = uiState.currentUserId,
                onDismiss = { viewModel.closeComments() },
                onAddComment = { c -> viewModel.addComment(uiState.commentsPostId!!, c) },
                onDeleteComment = { cId -> viewModel.deleteComment(cId, uiState.commentsPostId!!) }
            )
        }

        // Stories Viewer Modal
        if (storiesState.isStoryViewerOpen && storiesState.currentViewerUser != null) {
            StoryViewerDialog(
                user = storiesState.currentViewerUser!!,
                stories = storiesState.userStories,
                currentIndex = storiesState.currentStoryIndex,
                isOwner = storiesState.currentViewerUser?.userId == uiState.currentUserId,
                onNext = { storiesViewModel.nextStory() },
                onPrevious = { storiesViewModel.previousStory() },
                onClose = { storiesViewModel.closeStoryViewer() },
                onDelete = { storiesViewModel.deleteCurrentStory() },
                onReact = { rx -> storiesViewModel.reactToStory(rx) },
                onComment = { msg -> storiesViewModel.commentOnStory(msg) },
                viewModel = storiesViewModel
            )
        }

        // Create Story Dialog
        CreateStoryDialog(
            open = storiesState.isCreatingStory,
            onClose = { storiesViewModel.closeCreateStory() },
            onPublish = { content, mediaUrl, mediaType, background ->
                storiesViewModel.publishStory(content, mediaUrl, mediaType, background)
            }
        )
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPostModel,
    isOwner: Boolean,
    isSaved: Boolean,
    onReact: (String) -> Unit,
    onOpenComments: () -> Unit,
    onToggleSave: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReactionsRow by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!post.authorAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.authorUsername?.take(1) ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorUsername ?: "مستخدم",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = post.createdAt.take(10),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF0F172A))
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "إلغاء الحفظ" else "حفظ المنشور", color = Color.White) },
                            onClick = { onToggleSave(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White) }
                        )
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text("حذف المنشور", color = Color.Red) },
                                onClick = { onDelete(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("إبلاغ", color = Color.Yellow) },
                                onClick = { onReport(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Yellow) }
                            )
                        }
                    }
                }
            }

            // Post Content
            if (!post.content.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = post.content,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Media attachment
            if (!post.mediaUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentScale = ContentScale.Fit
                )
            }

            // Reaction bar / Emojis selection popup
            if (showReactionsRow) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    REACTIONS_LIST.forEach { (key, emoji) ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable {
                                    onReact(key)
                                    showReactionsRow = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Action Buttons (React, Comment, Share, Save)
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // React Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showReactionsRow = !showReactionsRow }
                        .padding(4.dp)
                ) {
                    Text(
                        text = when (post.myReaction) {
                            "love" -> "❤️"
                            "haha" -> "😂"
                            "wow" -> "😮"
                            "sad" -> "😢"
                            "angry" -> "😡"
                            else -> "👍"
                        },
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.reactionsCount > 0) "${post.reactionsCount}" else "تفاعل",
                        color = if (post.myReaction != null) Color(0xFF059669) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onOpenComments() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "تعليق",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.commentsCount > 0) "${post.commentsCount} تعليق" else "تعليق",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // Bookmark Button
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "حفظ",
                        tint = if (isSaved) Color(0xFFFBBF24) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    comments: List<CommunityCommentModel>,
    isLoading: Boolean,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "التعليقات (${comments.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF059669))
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد تعليقات بعد — كن أول من يعلق!", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!c.authorAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = c.authorAvatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF059669)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(c.authorUsername?.take(1) ?: "U", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = c.authorUsername ?: "مستخدم",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (c.authorId == currentUserId) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteComment(c.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = c.content,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add comment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("اكتب تعليقاً...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF059669),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(newCommentText)
                            newCommentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF059669), CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
