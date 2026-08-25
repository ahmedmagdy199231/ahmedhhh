package app.lovable.giant.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LogOut
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lovable.giant.data.models.UserProfile
import app.lovable.giant.ui.theme.BorderColor
import app.lovable.giant.ui.theme.CardBg
import app.lovable.giant.ui.theme.DarkBg
import app.lovable.giant.ui.theme.ErrorRed
import app.lovable.giant.ui.theme.PrimaryEmerald
import app.lovable.giant.ui.theme.TextPrimary
import app.lovable.giant.ui.theme.TextSecondary
import coil.compose.AsyncImage

val ARAB_COUNTRIES = listOf(
    "المملكة العربية السعودية", "مصر", "الإمارات العربية المتحدة", "الكويت", "قطر",
    "البحرين", "سلطنة عُمان", "الأردن", "العراق", "سوريا", "لبنان", "فلسطين",
    "اليمن", "السودان", "ليبيا", "تونس", "الجزائر", "المغرب", "موريتانيا", "أخرى"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            onLoggedOut()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAvatarFromUri(context, it) }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الملف الشخصي",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.LogOut, contentDescription = "Logout", tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBg)
        ) {
            when (val s = uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryEmerald)
                    }
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = ErrorRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadProfile() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text("إعادة المحاولة")
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        profile = s.profile,
                        isSaving = isSaving,
                        isUploadingAvatar = isUploadingAvatar,
                        onPickAvatar = { imagePickerLauncher.launch("image/*") },
                        onSaveInfo = { bio, gender, country ->
                            viewModel.updateProfileInfo(bio, gender, country)
                        },
                        onToggleHideLastSeen = { viewModel.togglePrivacy(hideLastSeen = it) },
                        onToggleDmLocked = { viewModel.togglePrivacy(dmLocked = it) },
                        onOpenEmailDialog = { showEmailDialog = true },
                        onOpenPasswordDialog = { showPasswordDialog = true },
                        onOpenAboutDialog = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("هل أنت متأكد من رغبتك في تسجيل الخروج من Giant؟", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("تسجيل الخروج", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = CardBg
        )
    }

    // Change Password Dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { newPass ->
                showPasswordDialog = false
                viewModel.changePassword(newPass)
            }
        )
    }

    // Change Email Dialog
    if (showEmailDialog) {
        val currentProfile = (uiState as? ProfileUiState.Success)?.profile
        ChangeEmailDialog(
            currentEmail = currentProfile?.email ?: "",
            onDismiss = { showEmailDialog = false },
            onConfirm = { newEmail ->
                showEmailDialog = false
                viewModel.changeEmail(newEmail)
            }
        )
    }

    // About Giant Dialog
    if (showAboutDialog) {
        AboutGiantDialog(onDismiss = { showAboutDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    profile: UserProfile,
    isSaving: Boolean,
    isUploadingAvatar: Boolean,
    onPickAvatar: () -> Unit,
    onSaveInfo: (bio: String, gender: String?, country: String?) -> Unit,
    onToggleHideLastSeen: (Boolean) -> Unit,
    onToggleDmLocked: (Boolean) -> Unit,
    onOpenEmailDialog: () -> Unit,
    onOpenPasswordDialog: () -> Unit,
    onOpenAboutDialog: () -> Unit
) {
    var bioText by remember(profile.bio) { mutableStateOf(profile.bio ?: "") }
    var selectedGender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var selectedCountry by remember(profile.country) { mutableStateOf(profile.country ?: "") }
    var isCountryMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card (Cover & Avatar & Points)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Cover Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF0F172A))
                                )
                            )
                    )

                    // Avatar + Basic Info Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with Upload Button
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(3.dp, PrimaryEmerald, CircleShape)
                                .clickable { onPickAvatar() }
                        ) {
                            if (!profile.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PrimaryEmerald.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.username.take(1).uppercase(),
                                        color = PrimaryEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    )
                                }
                            }

                            // Camera overlay badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Username, Points, Level & VIP
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.username,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (profile.isVip || profile.points > 10000) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFDC2626))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "★ VIP",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "المستوى ${profile.level} • ${profile.points} نقطة",
                                fontSize = 13.sp,
                                color = PrimaryEmerald,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${profile.profileViews} مشاهدة للملف",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bio & Info Form
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "النبذة الشخصية (Bio)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bioText,
                        onValueChange = { if (it.length <= 160) bioText = it },
                        placeholder = { Text("اكتب نبذة عن نفسك...", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Text(
                        text = "${bioText.length}/160",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender Selector
                    Text(
                        text = "الجنس",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { selectedGender = "male" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedGender == "male") PrimaryEmerald else DarkBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("♂ ذكر", color = if (selectedGender == "male") Color.White else TextPrimary)
                        }

                        Button(
                            onClick = { selectedGender = "female" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedGender == "female") PrimaryEmerald else DarkBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("♀ أنثى", color = if (selectedGender == "female") Color.White else TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Country Dropdown
                    Text(
                        text = "البلد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isCountryMenuExpanded,
                        onExpandedChange = { isCountryMenuExpanded = !isCountryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCountry.ifEmpty { "اختر البلد" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryMenuExpanded) },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = PrimaryEmerald) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryEmerald,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isCountryMenuExpanded,
                            onDismissRequest = { isCountryMenuExpanded = false },
                            modifier = Modifier.background(CardBg)
                        ) {
                            ARAB_COUNTRIES.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country, color = TextPrimary) },
                                    onClick = {
                                        selectedCountry = country
                                        isCountryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = { onSaveInfo(bioText, selectedGender, selectedCountry) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("حفظ التغييرات", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Privacy Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الخصوصية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryEmerald
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("قفل الرسائل الخاصة", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("فقط الأصدقاء يمكنهم مراسلتك", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = profile.dmLocked,
                            onCheckedChange = onToggleDmLocked,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryEmerald,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = DarkBg
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("إخفاء آخر ظهور", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("لن يظهر آخر وقت كنت فيه نشطاً", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = profile.hideLastSeen,
                            onCheckedChange = onToggleHideLastSeen,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryEmerald,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = DarkBg
                            )
                        )
                    }
                }
            }
        }

        // Account & Security
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الحساب والأمان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryEmerald
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Email row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEmailDialog() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("البريد الإلكتروني", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(profile.email ?: "لم يتم تعيين بريد", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }

                    if (!profile.pendingEmail.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF854D0E).copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "بانتظار تأكيد البريد الجديد: ${profile.pendingEmail}",
                                fontSize = 11.sp,
                                color = Color(0xFFFDE047)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

                    // Password row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPasswordDialog() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("تغيير كلمة المرور", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

                    // About row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAboutDialog() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("حول تطبيق Giant", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                        Text("v11.6.0", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير كلمة المرور", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("كلمة المرور الجديدة", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("تأكيد كلمة المرور", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.length < 6) {
                        error = "يجب أن تكون كلمة المرور 6 أحرف على الأقل"
                    } else if (password != confirmPassword) {
                        error = "كلمتا المرور غير متطابقتين"
                    } else {
                        onConfirm(password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير البريد الإلكتروني", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("البريد الحالي: $currentEmail", color = TextSecondary, fontSize = 13.sp)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("البريد الإلكتروني الجديد", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        error = "يرجى إدخال بريد إلكتروني صالح"
                    } else {
                        onConfirm(email)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Text("إرسال رابط التأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun AboutGiantDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Giant Chat", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الإصدار: 11.6.0 (Native Edition)", color = PrimaryEmerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("تطبيق Giant الصوتي والدردشة الاجتماعية السريعة والآمنة.", color = TextSecondary, fontSize = 13.sp)
                Text("جميع الحقوق محفوظة © Giant", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Text("حسناً")
            }
        },
        containerColor = CardBg
    )
}
