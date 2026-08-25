package app.lovable.giant.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.lovable.giant.data.models.StoryUserModel
import coil.compose.AsyncImage

private val BACKGROUNDS = listOf(
    "linear-gradient(135deg,#0f172a,#1e293b)",
    "linear-gradient(135deg,#7c3aed,#ec4899)",
    "linear-gradient(135deg,#f59e0b,#ef4444)",
    "linear-gradient(135deg,#10b981,#0891b2)",
    "linear-gradient(135deg,#1e3a8a,#0ea5e9)",
    "linear-gradient(135deg,#be185d,#7e22ce)"
)

@Composable
fun CreateStoryDialog(
    open: Boolean,
    onClose: () -> Unit,
    onPublish: (content: String?, mediaUrl: String?, mediaType: String?, background: String?) -> Unit
) {
    if (!open) return

    var content by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var selectedBg by remember { mutableStateOf(BACKGROUNDS[0]) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إنشاء قصة جديدة ✨",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Preview Box
                val bgBrush = when {
                    selectedBg.contains("7c3aed") -> Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                    selectedBg.contains("f59e0b") -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
                    selectedBg.contains("10b981") -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0891B2)))
                    selectedBg.contains("1e3a8a") -> Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0EA5E9)))
                    selectedBg.contains("be185d") -> Brush.linearGradient(listOf(Color(0xFFBE185D), Color(0xFF7E22CE)))
                    else -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgBrush)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (content.isNotEmpty()) {
                        Text(
                            text = content,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "معاينة القصة...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Background gradient selectors
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BACKGROUNDS) { bg ->
                        val bBrush = when {
                            bg.contains("7c3aed") -> Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                            bg.contains("f59e0b") -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
                            bg.contains("10b981") -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF0891B2)))
                            bg.contains("1e3a8a") -> Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0EA5E9)))
                            bg.contains("be185d") -> Brush.linearGradient(listOf(Color(0xFFBE185D), Color(0xFF7E22CE)))
                            else -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(bBrush)
                                .clickable { selectedBg = bg }
                                .then(
                                    if (selectedBg == bg) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 280) content = it },
                    label = { Text("نص القصة", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("ماذا يدور في ذهنك؟", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
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
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("رابط صورة/فيديو (اختياري)", color = Color.White.copy(alpha = 0.7f)) },
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (content.isNotBlank() || mediaUrl.isNotBlank()) {
                                val mType = if (mediaUrl.endsWith(".mp4") || mediaUrl.contains("video")) "video" else if (mediaUrl.isNotBlank()) "image" else null
                                onPublish(content, mediaUrl.ifBlank { null }, mType, selectedBg)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("نشر", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesBar(
    stories: List<StoryUserModel>,
    myUserId: String,
    onUserClick: (StoryUserModel) -> Unit,
    onCreateClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // Add my story button
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCreateClick() }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.5.dp, Color(0xFF059669), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "قصة جديدة",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "قصتك",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Active user stories
        items(stories) { user ->
            val isMe = user.userId == myUserId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onUserClick(user) }
                    .padding(2.dp)
            ) {
                val ringBrush = if (user.hasUnseen) {
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFF59E0B),
                            Color(0xFFEF4444),
                            Color(0xFFEC4899),
                            Color(0xFF8B5CF6),
                            Color(0xFF3B82F6),
                            Color(0xFF10B981),
                            Color(0xFFF59E0B)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f)))
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ringBrush)
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.username?.take(1) ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isMe) "أنت" else (user.username ?: "مستخدم"),
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
