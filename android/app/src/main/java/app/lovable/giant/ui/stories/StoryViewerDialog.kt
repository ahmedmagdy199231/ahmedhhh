package app.lovable.giant.ui.stories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lovable.giant.data.models.StoryItemModel
import app.lovable.giant.data.models.StoryUserModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val QUICK_EMOJIS = listOf("❤️", "🔥", "😂", "😮", "😢", "👏", "💯")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerDialog(
    user: StoryUserModel,
    stories: List<StoryItemModel>,
    currentIndex: Int,
    isOwner: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onComment: (String) -> Unit,
    viewModel: StoriesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStory = if (currentIndex in stories.indices) stories[currentIndex] else null
    var commentText by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Auto-advance timer (5 seconds)
    LaunchedEffect(currentIndex, stories.size, isPaused) {
        if (stories.isEmpty() || isPaused) return@LaunchedEffect
        progress = 0f
        val step = 50L
        val total = 5000L
        var elapsed = 0L
        while (elapsed < total) {
            delay(step)
            if (!isPaused) {
                elapsed += step
                progress = elapsed.toFloat() / total.toFloat()
            }
        }
        onNext()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.35f) {
                                onPrevious()
                            } else {
                                onNext()
                            }
                        }
                    )
                }
        ) {
            // Background / Story media content
            if (currentStory != null) {
                if (!currentStory.mediaUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentStory.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val bgBrush = parseBackgroundGradient(currentStory.background)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentStory.content.isNullOrEmpty()) {
                            Text(
                                text = currentStory.content,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                }

                // Overlay Text if media is present
                if (!currentStory.mediaUrl.isNullOrEmpty() && !currentStory.content.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = currentStory.content,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Top Header: Progress bars & User profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                // Progress Bars Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in stories.indices) {
                        val barProgress = when {
                            i < currentIndex -> 1f
                            i == currentIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }

                // User Info & Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!user.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.username?.take(1) ?: "G",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = user.username ?: "مستخدم",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOwner) "قصتك" else "قصة نشطة",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwner) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف",
                                    tint = Color.Red
                                )
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Controls (Owner: Views counter / Visitor: Reaction & Comment)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(16.dp)
            ) {
                if (isOwner) {
                    Button(
                        onClick = { viewModel.toggleViewsSheet() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.storyViews.size} مشاهدة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Quick emojis
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QUICK_EMOJIS.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .clickable { onReact(emoji) }
                                        .padding(4.dp)
                                )
                            }
                        }

                        // Direct message reply input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("رد على القصة في الخاص...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        onComment(commentText)
                                        commentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFF059669), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "إرسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Views BottomSheet for owner
            if (isOwner && uiState.isViewsSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.toggleViewsSheet() },
                    containerColor = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "مشاهدو القصة (${uiState.storyViews.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (uiState.storyViews.isEmpty()) {
                            Text(
                                text = "لا توجد مشاهدات بعد",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                items(uiState.storyViews) { view ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!view.avatarUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = view.avatarUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF059669)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = view.username?.take(1) ?: "?",
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = view.username ?: "مستخدم",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseBackgroundGradient(bg: String?): Brush {
    return when {
        bg?.contains("7c3aed") == true -> Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
        bg?.contains("f59e0b") == true -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
        bg?.contains("10b981") == true -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0891B2)))
        bg?.contains("1e3a8a") == true -> Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0EA5E9)))
        bg?.contains("be185d") == true -> Brush.linearGradient(listOf(Color(0xFFBE185D), Color(0xFF7E22CE)))
        else -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
    }
}
