package app.lovable.giant.ui.rooms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronDown
import androidx.compose.material.icons.filled.ChevronUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lovable.giant.media.NativeRoomMusicController
import app.lovable.giant.ui.theme.BorderColor
import app.lovable.giant.ui.theme.CardBg
import app.lovable.giant.ui.theme.ErrorRed
import app.lovable.giant.ui.theme.PrimaryEmerald
import app.lovable.giant.ui.theme.TextPrimary
import app.lovable.giant.ui.theme.TextSecondary
import coil.compose.AsyncImage

@Composable
fun RoomMusicPlayerBar(
    musicController: NativeRoomMusicController,
    modifier: Modifier = Modifier
) {
    val uiState by musicController.uiState.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableFloatStateOf(0f) }

    val track = uiState.music?.current
    val isPaused = uiState.music?.paused ?: false
    val durationMs = track?.durationMs ?: 0L
    val currentPosMs = if (isScrubbing) scrubPos.toLong() else uiState.localPlaybackPosMs

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Search / Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن مقطع موسيقي...", fontSize = 12.sp, color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank() && !uiState.isSearching) {
                            musicController.searchAndPlay(searchQuery.trim()) { ok ->
                                if (ok) searchQuery = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryEmerald)
                ) {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ChevronUp else Icons.Default.ChevronDown,
                        contentDescription = "Expand",
                        tint = TextSecondary
                    )
                }
            }

            // Track details and player controls (collapsible)
            AnimatedVisibility(
                visible = isExpanded && track != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (track != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        // Track info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (track.artwork.isNotBlank()) {
                                AsyncImage(
                                    model = track.artwork,
                                    contentDescription = track.title,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryEmerald.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = PrimaryEmerald)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist}${if (!track.requesterName.isNullOrBlank()) " • طلب ${track.requesterName}" else ""}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isPaused) "⏸ متوقفة مؤقتاً" else "▶️ قيد التشغيل (ExoPlayer Native)",
                                    fontSize = 10.sp,
                                    color = if (isPaused) Color(0xFFF59E0B) else PrimaryEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Progress Slider
                        if (durationMs > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatMs(currentPosMs),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                                Slider(
                                    value = currentPosMs.coerceIn(0L, durationMs).toFloat(),
                                    onValueChange = {
                                        isScrubbing = true
                                        scrubPos = it
                                    },
                                    onValueChangeFinished = {
                                        isScrubbing = false
                                        musicController.seekTo(scrubPos.toLong())
                                    },
                                    valueRange = 0f..durationMs.toFloat(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryEmerald,
                                        activeTrackColor = PrimaryEmerald,
                                        inactiveTrackColor = BorderColor
                                    )
                                )
                                Text(
                                    text = formatMs(durationMs),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Player Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rewind 10s
                            IconButton(
                                onClick = { musicController.seekTo((currentPosMs - 10000).coerceAtLeast(0L)) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }

                            // Play / Pause
                            IconButton(
                                onClick = { musicController.togglePlayPause() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryEmerald)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }

                            // Fast Forward 10s
                            IconButton(
                                onClick = { musicController.seekTo((currentPosMs + 10000).coerceAtMost(durationMs)) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }

                            // Skip
                            IconButton(
                                onClick = { musicController.skipTrack() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }

                            // Stop
                            IconButton(
                                onClick = { musicController.stopTrack() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Stop", tint = ErrorRed, modifier = Modifier.size(20.dp))
                            }

                            // Mute
                            IconButton(
                                onClick = { musicController.toggleMute() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = if (uiState.isMuted) ErrorRed else PrimaryEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Lock
                            IconButton(
                                onClick = { musicController.toggleLock() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock",
                                    tint = if (uiState.isLocked) ErrorRed else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
