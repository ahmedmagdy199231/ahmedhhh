package app.lovable.giant.ui.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lovable.giant.data.models.GiftCatalogModel
import app.lovable.giant.data.models.ShopItemModel
import app.lovable.giant.ui.theme.BorderColor
import app.lovable.giant.ui.theme.CardBg
import app.lovable.giant.ui.theme.DarkBg
import app.lovable.giant.ui.theme.ErrorRed
import app.lovable.giant.ui.theme.PrimaryEmerald
import app.lovable.giant.ui.theme.TextPrimary
import app.lovable.giant.ui.theme.TextSecondary

enum class StoreTabKey(val label: String, val icon: ImageVector) {
    BADGES("الشارات", Icons.Default.EmojiEvents),
    AVATAR_FRAMES("إطارات البروفايل", Icons.Default.Diamond),
    ENTRY_EFFECTS("مؤثرات الدخول", Icons.Default.AutoAwesome),
    CHAT_EFFECTS("مؤثرات الدردشة", Icons.Default.Whatshot),
    NAME_COLORS("ألوان الاسم", Icons.Default.Star),
    CHAT_COLORS("ألوان الخط", Icons.Default.Star),
    GIFTS("الهدايا", Icons.Default.CardGiftcard)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: StoreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val busyItemId by viewModel.busyItemId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(StoreTabKey.BADGES) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المتجر",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "كل المؤثرات والشارات والهدايا في مكان واحد",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (uiState is StoreUiState.Success) {
                        val points = (uiState as StoreUiState.Success).points
                        Row(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFBBF24), Color(0xFFEAB308))
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = points.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
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
                is StoreUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryEmerald)
                    }
                }
                is StoreUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = ErrorRed, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadStoreData() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إعادة المحاولة")
                        }
                    }
                }
                is StoreUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Official seller card
                        PointsSellerCard(adminUsername = s.adminUsername)

                        // Store Tabs
                        StoreTabsRow(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )

                        // Content
                        if (selectedTab == StoreTabKey.GIFTS) {
                            GiftsCatalogGrid(gifts = s.gifts)
                        } else {
                            val filteredItems = remember(s.items, selectedTab) {
                                when (selectedTab) {
                                    StoreTabKey.BADGES -> s.items.filter { it.kind == "badge" }
                                    StoreTabKey.AVATAR_FRAMES -> s.items.filter { it.kind == "avatar_frame" }
                                    StoreTabKey.ENTRY_EFFECTS -> s.items.filter { it.kind == "effect" && it.code.startsWith("entry_") }
                                    StoreTabKey.CHAT_EFFECTS -> s.items.filter { it.kind == "effect" && !it.code.startsWith("entry_") }
                                    StoreTabKey.NAME_COLORS -> s.items.filter { it.kind == "name_color" }
                                    StoreTabKey.CHAT_COLORS -> s.items.filter { it.kind == "chat_color" }
                                    StoreTabKey.GIFTS -> emptyList()
                                }
                            }

                            if (selectedTab == StoreTabKey.ENTRY_EFFECTS) {
                                Text(
                                    text = "مؤثرات تُعرض تلقائياً عند دخولك أي غرفة وعند فتح بروفايلك. بعضها مخصص للذكور وبعضها للإناث.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            if (filteredItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد عناصر في هذا القسم بعد",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        val isOwned = s.ownedItemIds.contains(item.id)
                                        val isEquipped = s.equippedMap[item.kind] == item.id
                                        val genderLocked = item.genderTarget != null && s.myGender != null && s.myGender != item.genderTarget
                                        val isBusy = busyItemId == item.id

                                        ShopItemCard(
                                            item = item,
                                            isOwned = isOwned,
                                            isEquipped = isEquipped,
                                            genderLocked = genderLocked,
                                            canAfford = s.points >= item.price,
                                            isBusy = isBusy,
                                            onBuy = { viewModel.buyItem(item) },
                                            onToggleEquip = { viewModel.toggleEquip(item) }
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

@Composable
fun PointsSellerCard(adminUsername: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "شراء النقاط",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "راسل بائع النقاط الرسمي (@$adminUsername) لشحن رصيدك واستخدامه في شراء الشارات والمؤثرات أو إرسال الهدايا.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun StoreTabsRow(
    selectedTab: StoreTabKey,
    onTabSelected: (StoreTabKey) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StoreTabKey.values().forEach { tab ->
            val isSelected = selectedTab == tab
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PrimaryEmerald else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItemModel,
    isOwned: Boolean,
    isEquipped: Boolean,
    genderLocked: Boolean,
    canAfford: Boolean,
    isBusy: Boolean,
    onBuy: () -> Unit,
    onToggleEquip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isEquipped) PrimaryEmerald else BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gender badge if any
            if (item.genderTarget != null) {
                val isFemale = item.genderTarget == "female"
                Row(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFemale) Color(0x33EC4899) else Color(0x333B82F6))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isFemale) Icons.Default.Female else Icons.Default.Male,
                        contentDescription = null,
                        tint = if (isFemale) Color(0xFFF472B6) else Color(0xFF60A5FA),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isFemale) "إناث" else "ذكور",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFemale) Color(0xFFF472B6) else Color(0xFF60A5FA)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Visual Preview
            ItemPreview(item = item)

            Spacer(modifier = Modifier.height(8.dp))

            // Item Name
            Text(
                text = item.nameAr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Item Price
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = item.price.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAB308)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button
            if (isOwned) {
                Button(
                    onClick = onToggleEquip,
                    enabled = !isBusy && !genderLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEquipped) Color(0xFF16A34A) else PrimaryEmerald.copy(alpha = 0.2f),
                        contentColor = if (isEquipped) Color.White else PrimaryEmerald
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (genderLocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("غير متاح", fontSize = 11.sp)
                    } else if (isEquipped) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مُطبَّق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("تطبيق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onBuy,
                    enabled = !isBusy && !genderLocked && canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (genderLocked) {
                        Text("غير متاح", fontSize = 11.sp)
                    } else if (!canAfford) {
                        Text("نقاط غير كافية", fontSize = 10.sp)
                    } else {
                        Text("شراء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemPreview(item: ShopItemModel) {
    val color = parseHexColor(item.colorHex) ?: Color(0xFFEF4444)

    when (item.kind) {
        "badge" -> {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        item.code.contains("diamond") -> Icons.Default.Diamond
                        item.code.contains("verified") -> Icons.Default.CheckCircle
                        item.code.contains("fire") -> Icons.Default.Whatshot
                        item.code.contains("star") -> Icons.Default.Star
                        else -> Icons.Default.EmojiEvents
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = item.nameAr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        "name_color" -> {
            Text(
                text = "اسمك",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
        "chat_color" -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryEmerald.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "مرحباً 👋",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        "avatar_frame" -> {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryEmerald, Color(0xFFD946EF), Color(0xFFF59E0B))
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CardBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 22.sp)
                }
            }
        }
        "effect" -> {
            val emoji = when {
                item.code.contains("dragon") -> "🐉"
                item.code.contains("princess") -> "👸"
                item.code.contains("knight") -> "🛡️"
                item.code.contains("magic") -> "✨"
                item.code.contains("mascot") -> "🎉"
                item.code.contains("portal") -> "🌀"
                else -> item.previewEmoji ?: "✨"
            }
            Text(text = emoji, fontSize = 34.sp)
        }
        else -> {
            Text(text = "✨", fontSize = 30.sp)
        }
    }
}

@Composable
fun GiftsCatalogGrid(gifts: List<GiftCatalogModel>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x22F472B6)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44F472B6))
        ) {
            Text(
                text = "🎁 الهدايا تُرسل لأعضاء آخرين من داخل الغرف الصوتية. افتح أي غرفة واضغط على أيقونة الهدية لإرسالها.",
                fontSize = 11.sp,
                color = Color(0xFFF472B6),
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(gifts, key = { it.id }) { g ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (g.scope == "global") {
                            Text(
                                text = "عالمي 🌍",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEAB308))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(text = g.emoji ?: "🎁", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = g.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = g.costPoints.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEAB308)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return null
        }
        Color(colorInt)
    } catch (e: Exception) {
        null
    }
}
