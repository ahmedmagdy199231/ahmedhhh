package app.lovable.giant.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lovable.giant.data.models.MusicTrackModel
import app.lovable.giant.media.GiantAudioPlayer
import app.lovable.giant.ui.theme.BorderColor
import app.lovable.giant.ui.theme.CardBg
import app.lovable.giant.ui.theme.PrimaryEmerald
import app.lovable.giant.ui.theme.TextPrimary
import app.lovable.giant.ui.theme.TextSecondary
import coil.compose.AsyncImage
import org.json.JSONObject

object TrackDMParser {
    fun tryParseTrackDM(content: String): Pair<MusicTrackModel, String>? {
        if (!content.startsWith("🎵TRACK::")) return null
        val body = content.removePrefix("🎵TRACK::")
        val sep = body.lastIndexOf("::")
        if (sep < 0) return null
        val jsonPart = body.substring(0, sep)
        val senderName = body.substring(sep + 2)

        return try {
            val json = JSONObject(jsonPart)
            val previewUrl = json.optString("preview_url")
            val title = json.optString("title")
            if (previewUrl.isBlank() || title.isBlank()) return null

            val track = MusicTrackModel(
                videoId = json.optString("videoId").takeIf { it.isNotBlank() },
                title = title,
                artist = json.optString("artist", "فنان غير معروف"),
                artwork = json.optString("artwork", ""),
                previewUrl = previewUrl,
                durationMs = json.optLong("duration_ms", 0)
            )
            Pair(track, senderName)
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun TrackDMPlayerView(
    track: MusicTrackModel,
    senderName: String,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioPlayer = GiantAudioPlayer.getInstance(context)
    val playbackState by audioPlayer.playbackState.collectAsState()

    val isCurrentTrack = playbackState.currentUrl == track.previewUrl
    val isPlaying = isCurrentTrack && playbackState.isPlaying
    val isBuffering = isCurrentTrack && playbackState.isBuffering

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isMine) PrimaryEmerald.copy(alpha = 0.15f) else CardBg)
            .border(1.dp, if (isMine) PrimaryEmerald.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "أغنية مشاركة من $senderName",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEmerald
                )
            }

            Spacer(modifier = Modifier.size(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track.artwork.isNotBlank()) {
                    AsyncImage(
                        model = track.artwork,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        audioPlayer.togglePlayPause(track.previewUrl)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryEmerald)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
