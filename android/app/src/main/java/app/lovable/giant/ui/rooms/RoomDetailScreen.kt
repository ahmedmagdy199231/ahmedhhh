package app.lovable.giant.ui.rooms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lovable.giant.data.models.ChatMessage
import app.lovable.giant.ui.gifts.GiftPickerBottomSheet
import app.lovable.giant.ui.theme.BorderColor
import app.lovable.giant.ui.theme.CardBg
import app.lovable.giant.ui.theme.DarkBg
import app.lovable.giant.ui.theme.ErrorRed
import app.lovable.giant.ui.theme.PrimaryEmerald
import app.lovable.giant.ui.theme.TextPrimary
import app.lovable.giant.ui.theme.TextSecondary
import app.lovable.giant.webrtc.models.AudioRoute
import app.lovable.giant.webrtc.models.RaisedHand
import app.lovable.giant.webrtc.models.RoomSpeaker
import app.lovable.giant.webrtc.models.VoiceConnectionState
import app.lovable.giant.webrtc.models.VoiceRoomState
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SingleRoomViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SingleRoomViewModel(roomId) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showGiftPicker by remember { mutableStateOf(false) }
    var showRaisedHandsDialog by remember { mutableStateOf(false) }
    var selectedSpeakerForActions by remember { mutableStateOf<RoomSpeaker?>(null) }

    // Permission launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.joinStage()
        }
    }

    fun requestMicAndJoinStage() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.joinStage()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showGiftPicker) {
        GiftPickerBottomSheet(
            roomId = roomId,
            onDismiss = { showGiftPicker = false }
        )
    }

    // Moderation sheet/dialog for selected speaker
    selectedSpeakerForActions?.let { sp ->
        AlertDialog(
            onDismissRequest = { selectedSpeakerForActions = null },
            title = { Text(text = sp.username ?: "متحدث في المنصة", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(text = "إدارة المتحدث في المنصة الصوتية", color = TextSecondary)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.muteSpeaker(sp.userId, !sp.isMuted)
                        selectedSpeakerForActions = null
                    }
                ) {
                    Text(if (sp.isMuted) "إلغاء كتم المتحدث" else "كتم المتحدث", color = PrimaryEmerald)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.kickSpeaker(sp.userId)
                        selectedSpeakerForActions = null
                    }
                ) {
                    Text("إنزال من المنصة", color = ErrorRed)
                }
            },
            containerColor = CardBg
        )
    }

    // Raised hands list dialog for mods
    if (showRaisedHandsDialog) {
        RaisedHandsDialog(
            hands = voiceState.raisedHands,
            onInvite = { uid ->
                viewModel.inviteUser(uid)
                viewModel.lowerHand()
            },
            onDismiss = { showRaisedHandsDialog = false }
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (val s = uiState) {
                                is SingleRoomUiState.Success -> s.room.name
                                else -> voiceState.roomName ?: "الغرفة الصوتية"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (voiceState.connectionState) {
                                            VoiceConnectionState.CONNECTED -> PrimaryEmerald
                                            VoiceConnectionState.CONNECTING, VoiceConnectionState.RECONNECTING -> Color(0xFFF59E0B)
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (voiceState.connectionState) {
                                    VoiceConnectionState.CONNECTED -> "صوت Native مباشر"
                                    VoiceConnectionState.CONNECTING -> "جارٍ تهيئة WebRTC..."
                                    VoiceConnectionState.RECONNECTING -> "جارٍ إعادة الاتصال..."
                                    VoiceConnectionState.FAILED -> "فشل الاتصال"
                                    else -> "متصل"
                                },
                                fontSize = 11.sp,
                                color = when (voiceState.connectionState) {
                                    VoiceConnectionState.CONNECTED -> PrimaryEmerald
                                    VoiceConnectionState.CONNECTING, VoiceConnectionState.RECONNECTING -> Color(0xFFF59E0B)
                                    VoiceConnectionState.FAILED -> ErrorRed
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.leaveRoom(onNavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (voiceState.raisedHands.isNotEmpty()) {
                        IconButton(onClick = { showRaisedHandsDialog = true }) {
                            Box {
                                Icon(Icons.Default.FrontHand, contentDescription = "Raised Hands", tint = Color(0xFFF59E0B))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(ErrorRed)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = voiceState.raisedHands.size.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { showGiftPicker = true }) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Send Gift", tint = Color(0xFFEC4899))
                    }

                    IconButton(onClick = { viewModel.leaveRoom(onNavigateBack) }) {
                        Icon(Icons.Default.PhoneOff, contentDescription = "Leave Room", tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(CardBg)) {
                // Speaker Invite Banner if user received an invitation
                voiceState.myState.pendingInvite?.let {
                    InviteToStageBanner(
                        onAccept = { requestMicAndJoinStage() },
                        onReject = { viewModel.rejectInvite() }
                    )
                }

                // Voice Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic button (if on stage) or Stage / Hand button (if listener)
                    if (voiceState.myState.isOnStage) {
                        IconButton(
                            onClick = { viewModel.toggleMic() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (voiceState.myState.isMuted) CardBg else PrimaryEmerald.copy(alpha = 0.25f))
                                .border(1.dp, if (voiceState.myState.isMuted) BorderColor else PrimaryEmerald, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (voiceState.myState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (voiceState.myState.isMuted) ErrorRed else PrimaryEmerald
                            )
                        }
                    } else {
                        if (voiceState.myState.hasRaisedHand) {
                            OutlinedButton(
                                onClick = { viewModel.lowerHand() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.FrontHand, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أنزل اليد", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.raiseHand()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                modifier = Modifier.height(38.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FrontHand, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طلب صعود", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Direct Stage Join / Leave for speakers & mods
                    if (voiceState.myState.isOnStage) {
                        OutlinedButton(
                            onClick = { viewModel.leaveStage() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("نزول من المنصة", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { requestMicAndJoinStage() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryEmerald),
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اصعد للبث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Speaker Output Toggle (Speakerphone / Earpiece)
                    IconButton(
                        onClick = { viewModel.toggleSpeaker() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                            .border(1.dp, BorderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = when (voiceState.audioRoute) {
                                AudioRoute.SPEAKER -> Icons.Default.VolumeUp
                                AudioRoute.WIRED_HEADSET, AudioRoute.BLUETOOTH -> Icons.Default.Headset
                                else -> Icons.Default.VolumeOff
                            },
                            contentDescription = "Speaker",
                            tint = if (voiceState.isSpeakerOn) PrimaryEmerald else TextSecondary
                        )
                    }

                    // Send Gift Button
                    IconButton(
                        onClick = { showGiftPicker = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEC4899).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Gift",
                            tint = Color(0xFFEC4899)
                        )
                    }
                }

                // Chat Input Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("اكتب رسالة داخل الغرفة...", color = TextSecondary, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText.trim())
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryEmerald)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBg)
        ) {
            when (val s = uiState) {
                is SingleRoomUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryEmerald)
                    }
                }
                is SingleRoomUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = ErrorRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text("رجوع")
                        }
                    }
                }
                is SingleRoomUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Reconnection retry bar if WebRTC failed
                        AnimatedVisibility(
                            visible = voiceState.connectionState == VoiceConnectionState.FAILED,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ErrorRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("انقطع اتصال الصوت Native", color = ErrorRed, fontSize = 12.sp)
                                IconButton(onClick = { viewModel.retryConnection() }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                                }
                            }
                        }

                        // Native WebRTC Voice Stage Section
                        NativeVoiceStageSection(
                            speakers = voiceState.speakers,
                            speakingUsers = voiceState.speakingUsers,
                            onSpeakerClick = { sp -> selectedSpeakerForActions = sp }
                        )

                        // Messages Area
                        Text(
                            text = "الدردشة المباشرة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.messages, key = { it.id }) { msg ->
                                RoomMessageItem(msg = msg)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NativeVoiceStageSection(
    speakers: List<RoomSpeaker>,
    speakingUsers: Set<String>,
    onSpeakerClick: (RoomSpeaker) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "منصة البث الصوتي المباشر (${speakers.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (speakers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("مباشر ON AIR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (speakers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد متحدثون حالياً على المنصة. اضغط 'اصعد للبث' للبدء.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(speakers, key = { it.userId }) { sp ->
                        val isSpeaking = speakingUsers.contains(sp.userId)
                        SpeakerSeatItem(
                            speaker = sp,
                            isSpeaking = isSpeaking,
                            onClick = { onSpeakerClick(sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeakerSeatItem(
    speaker: RoomSpeaker,
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(CardBg)
                .border(
                    width = if (isSpeaking) 3.dp else 1.5.dp,
                    color = if (isSpeaking) PrimaryEmerald else BorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!speaker.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = speaker.avatarUrl,
                    contentDescription = speaker.username,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Mute / Speaking Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (speaker.isMuted) ErrorRed else PrimaryEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (speaker.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = speaker.username ?: "متحدث",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InviteToStageBanner(onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryEmerald.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryEmerald)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("دعوة للصعود إلى المنصة", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text("تمت دعوتك للتحدث عبر WebRTC Native", color = TextSecondary, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("قبول", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("رفض", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun RaisedHandsDialog(
    hands: List<RaisedHand>,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الأيدي المرفوعة (${hands.size})", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            if (hands.isEmpty()) {
                Text("لا توجد طلبات صعود حالياً.", color = TextSecondary)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(hands, key = { it.userId }) { h ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = h.username ?: "مستخدم", color = TextPrimary, fontSize = 13.sp)
                            Button(
                                onClick = { onInvite(h.userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("دعوة للمنصة", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = PrimaryEmerald)
            }
        },
        containerColor = DarkBg
    )
}
