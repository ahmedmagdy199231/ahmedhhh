package app.lovable.giant.ui.games

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lovable.giant.data.models.CharacterItem
import app.lovable.giant.data.models.CountryItem
import app.lovable.giant.data.models.FastQuestion
import app.lovable.giant.data.models.PatternItem
import app.lovable.giant.data.models.QuizQuestion
import app.lovable.giant.ui.theme.BorderColor
import app.lovable.giant.ui.theme.CardBg
import app.lovable.giant.ui.theme.DarkBg
import app.lovable.giant.ui.theme.ErrorRed
import app.lovable.giant.ui.theme.PrimaryEmerald
import app.lovable.giant.ui.theme.TextPrimary
import app.lovable.giant.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onNavigateBack: () -> Unit,
    viewModel: GamesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is GameEvent.WinAwarded -> {
                    // Handled inside toast
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ألعاب Giant Chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBg),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Stats Card
            item {
                GamesHeaderCard(
                    points = uiState.points,
                    wins = uiState.gameWins
                )
            }

            // Daily Reward Card
            item {
                DailyRewardCard(
                    isClaimed = uiState.isDailyClaimed,
                    onClaim = { viewModel.claimDailyReward() }
                )
            }

            item {
                Text(
                    text = "قائمة الألعاب التنافسية",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 1. Guess Number
            item {
                GuessNumberGameCard(onWin = { viewModel.recordWin("guess_number", 5) })
            }

            // 2. Quiz Game
            item {
                QuizGameCard(onWin = { viewModel.recordWin("quiz", 5) })
            }

            // 3. Scramble Word
            item {
                ScrambleWordGameCard(onWin = { viewModel.recordWin("scramble", 6) })
            }

            // 4. Fastest Answer
            item {
                FastestAnswerGameCard(onWin = { viewModel.recordWin("fastest", 4) })
            }

            // 5. Pattern Game
            item {
                PatternGameCard(onWin = { viewModel.recordWin("pattern", 6) })
            }

            // 6. Guess Character
            item {
                GuessCharacterGameCard(onWin = { viewModel.recordWin("character", 7) })
            }

            // 7. Guess Country
            item {
                GuessCountryGameCard(onWin = { viewModel.recordWin("country", 6) })
            }

            // 8. Lucky Box
            item {
                LuckyBoxGameCard(onWin = { pts -> viewModel.recordWin("lucky_box", pts) })
            }
        }
    }
}

@Composable
fun GamesHeaderCard(points: Long, wins: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF9333EA), Color(0xFF6366F1), Color(0xFF3B82F6))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎮", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تنافس واربح النقاط",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "كل فوز يُحسب مباشرة في رصيدك وإنجازاتك",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Points Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("رصيدك الحالي", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$points",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Wins Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("مستوى التحدي", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$wins",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyRewardCard(isClaimed: Boolean, onClaim: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isClaimed) { onClaim() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isClaimed) {
                        Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFE11D48)))
                    }
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الجائزة اليومية (10 - 30 نقطة)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (isClaimed) "تم استلامها اليوم — عد غداً للحصول على المزيد" else "اضغط هنا لاستلام مكافأتك اليومية فوراً!",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Icon(
                    imageVector = if (isClaimed) Icons.Default.Check else Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 1. 🎲 تخمين الرقم
// ----------------------------------------------------
@Composable
fun GuessNumberGameCard(onWin: () -> Unit) {
    var selectedNumber by remember { mutableStateOf<Int?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    fun play(n: Int) {
        selectedNumber = n
        val sysNumber = Random.nextInt(6) + 1
        if (n == sysNumber) {
            isSuccess = true
            resultText = "🎉 رائع! خمنت الرقم الصحيح $sysNumber (+5 نقاط)"
            onWin()
        } else {
            isSuccess = false
            resultText = "خسرت! الرقم كان $sysNumber (اخترت $n)"
        }
    }

    GameCardTemplate(
        emoji = "🎲",
        title = "تخمين الرقم",
        desc = "اختر رقماً من 1 إلى 6 واربح 5 نقاط",
        gradient = listOf(Color(0xFF10B981), Color(0xFF0D9488))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..6).forEach { num ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedNumber == num) PrimaryEmerald else Color(0xFF1E293B))
                        .clickable { play(num) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$num",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedNumber == num) Color.White else TextPrimary
                    )
                }
            }
        }

        resultText?.let { res ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSuccess) PrimaryEmerald.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = res,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) PrimaryEmerald else ErrorRed
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. ❓ سؤال وجواب
// ----------------------------------------------------
private val QUIZ_QUESTIONS = listOf(
    QuizQuestion("ما عاصمة اليابان؟", listOf("طوكيو", "بكين", "سيول", "بانكوك"), 0),
    QuizQuestion("من اخترع المصباح الكهربائي؟", listOf("نيوتن", "أديسون", "أينشتاين", "تسلا"), 1),
    QuizQuestion("كم عدد كواكب المجموعة الشمسية؟", listOf("7", "8", "9", "10"), 1),
    QuizQuestion("ما أكبر محيط في العالم؟", listOf("الأطلسي", "الهندي", "الهادئ", "المتجمد"), 2),
    QuizQuestion("في أي عام نزل الإنسان على القمر؟", listOf("1965", "1969", "1972", "1980"), 1),
    QuizQuestion("أي لغة برمجة طورها Brendan Eich؟", listOf("Python", "Java", "JavaScript", "C++"), 2),
    QuizQuestion("بطل كأس العالم 2022؟", listOf("البرازيل", "فرنسا", "الأرجنتين", "ألمانيا"), 2),
    QuizQuestion("مخرج فيلم Inception الشهير؟", listOf("سبيلبرغ", "كريستوفر نولان", "تارانتينو", "كاميرون"), 1)
)

@Composable
fun QuizGameCard(onWin: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(Random.nextInt(QUIZ_QUESTIONS.size)) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    val item = QUIZ_QUESTIONS[currentIndex]

    fun pick(idx: Int) {
        if (selectedOption != null) return
        selectedOption = idx
        if (idx == item.answerIndex) {
            onWin()
        }
    }

    fun next() {
        currentIndex = Random.nextInt(QUIZ_QUESTIONS.size)
        selectedOption = null
    }

    GameCardTemplate(
        emoji = "❓",
        title = "سؤال وجواب",
        desc = "ثقافة عامة • تكنولوجيا • رياضة (+5 نقاط)",
        gradient = listOf(Color(0xFF0284C7), Color(0xFF2563EB))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.options.chunked(2).forEach { rowOpts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowOpts.forEach { opt ->
                        val optIdx = item.options.indexOf(opt)
                        val isCorrect = optIdx == item.answerIndex
                        val isPicked = optIdx == selectedOption
                        val bg = when {
                            selectedOption == null -> Color(0xFF1E293B)
                            isCorrect -> PrimaryEmerald
                            isPicked -> ErrorRed
                            else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable(enabled = selectedOption == null) { pick(optIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOption != null && (isCorrect || isPicked)) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (selectedOption != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { next() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("سؤال جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// 3. 🧩 فك الحروف
// ----------------------------------------------------
private val WORDS_POOL = listOf(
    "برمجة", "كرسي", "نهار", "جامعة", "كتاب", "مطر", "شمس",
    "نجوم", "قمر", "صديق", "محبة", "حلم", "وردة", "بحر", "جبل"
)

@Composable
fun ScrambleWordGameCard(onWin: () -> Unit) {
    var word by remember { mutableStateOf(WORDS_POOL[Random.nextInt(WORDS_POOL.size)]) }
    var scrambled by remember { mutableStateOf(word.toList().shuffled().joinToString(" ")) }
    var inputGuess by remember { mutableStateOf("") }
    var resultStatus by remember { mutableStateOf<String?>(null) }

    fun next() {
        val nw = WORDS_POOL[Random.nextInt(WORDS_POOL.size)]
        word = nw
        scrambled = nw.toList().shuffled().joinToString(" ")
        inputGuess = ""
        resultStatus = null
    }

    fun submit() {
        if (inputGuess.trim() == word) {
            resultStatus = "correct"
            onWin()
        } else {
            resultStatus = "wrong"
        }
    }

    GameCardTemplate(
        emoji = "🧩",
        title = "فك الحروف",
        desc = "أعد ترتيب الحروف لاكتشاف الكلمة (+6 نقاط)",
        gradient = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = scrambled,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = PrimaryEmerald
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputGuess,
                onValueChange = { inputGuess = it },
                placeholder = { Text("اكتب الكلمة هنا...", fontSize = 13.sp, color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { submit() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("تحقق", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (resultStatus == "correct") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("🎉 إجابة صحيحة! أحسنت (+6 نقاط)", color = PrimaryEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else if (resultStatus == "wrong") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("حاول مجدداً، الكلمة غير صحيحة", color = ErrorRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(
            onClick = { next() },
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("كلمة أخرى", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ----------------------------------------------------
// 4. ⚡ أسرع إجابة
// ----------------------------------------------------
private val FAST_QUESTIONS = listOf(
    FastQuestion("2 + 2 × 3 = ؟", listOf("8", "10", "12"), 0),
    FastQuestion("أكبر عدد: 7، 12، 9", listOf("7", "12", "9"), 1),
    FastQuestion("نصف 50 + 10 = ؟", listOf("25", "35", "30"), 1),
    FastQuestion("كم يوماً في فبراير (سنة عادية)؟", listOf("28", "29", "30"), 0),
    FastQuestion("9 × 9 = ؟", listOf("72", "81", "99"), 1)
)

@Composable
fun FastestAnswerGameCard(onWin: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(Random.nextInt(FAST_QUESTIONS.size)) }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var pickedOption by remember { mutableStateOf<Int?>(null) }
    val item = FAST_QUESTIONS[currentIndex]

    fun pick(idx: Int) {
        if (pickedOption != null) return
        pickedOption = idx
        elapsedMs = System.currentTimeMillis() - startTime
        if (idx == item.answerIndex) {
            onWin()
        }
    }

    fun next() {
        currentIndex = Random.nextInt(FAST_QUESTIONS.size)
        startTime = System.currentTimeMillis()
        pickedOption = null
        elapsedMs = 0L
    }

    GameCardTemplate(
        emoji = "⚡",
        title = "أسرع إجابة",
        desc = "أجب بأسرع وقت ممكن لتفوز (+4 نقاط)",
        gradient = listOf(Color(0xFFEAB308), Color(0xFFEA580C))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item.options.forEachIndexed { idx, opt ->
                val isCorrect = idx == item.answerIndex
                val isPicked = idx == pickedOption
                val bg = when {
                    pickedOption == null -> Color(0xFF1E293B)
                    isCorrect -> PrimaryEmerald
                    isPicked -> ErrorRed
                    else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable(enabled = pickedOption == null) { pick(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pickedOption != null && (isCorrect || isPicked)) Color.White else TextPrimary
                    )
                }
            }
        }

        if (elapsedMs > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⏱ زمن الإجابة: $elapsedMs ms",
                fontSize = 11.sp,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.SemiBold
            )
        }

        if (pickedOption != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = { next() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تحدٍ جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// 5. 🔢 أكمل النمط
// ----------------------------------------------------
private val PATTERNS_LIST = listOf(
    PatternItem(listOf(2, 4, 6, 8), listOf(9, 10, 12), 1),
    PatternItem(listOf(1, 1, 2, 3, 5), listOf(7, 8, 9), 1),
    PatternItem(listOf(3, 6, 12, 24), listOf(36, 48, 60), 1),
    PatternItem(listOf(1, 4, 9, 16), listOf(20, 25, 30), 1),
    PatternItem(listOf(5, 10, 20, 40), listOf(60, 70, 80), 2)
)

@Composable
fun PatternGameCard(onWin: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(Random.nextInt(PATTERNS_LIST.size)) }
    var pickedOption by remember { mutableStateOf<Int?>(null) }
    val item = PATTERNS_LIST[currentIndex]

    fun pick(idx: Int) {
        if (pickedOption != null) return
        pickedOption = idx
        if (idx == item.answerIndex) {
            onWin()
        }
    }

    fun next() {
        currentIndex = Random.nextInt(PATTERNS_LIST.size)
        pickedOption = null
    }

    GameCardTemplate(
        emoji = "🔢",
        title = "أكمل النمط",
        desc = "ما الرقم التالي في السلسلة؟ (+6 نقاط)",
        gradient = listOf(Color(0xFF06B6D4), Color(0xFF2563EB))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${item.sequence.joinToString(" • ")} • ؟",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = PrimaryEmerald
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item.options.forEachIndexed { idx, opt ->
                val isCorrect = idx == item.answerIndex
                val isPicked = idx == pickedOption
                val bg = when {
                    pickedOption == null -> Color(0xFF1E293B)
                    isCorrect -> PrimaryEmerald
                    isPicked -> ErrorRed
                    else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable(enabled = pickedOption == null) { pick(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$opt",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pickedOption != null && (isCorrect || isPicked)) Color.White else TextPrimary
                    )
                }
            }
        }

        if (pickedOption != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { next() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("نمط جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// 6. 🎭 خمن الشخصية
// ----------------------------------------------------
private val CHARACTERS_LIST = listOf(
    CharacterItem(listOf("عالم فيزياء عبقري", "صاحب النظرية النسبية", "ألماني الأصل"), "أينشتاين", listOf("نيوتن", "أينشتاين", "تسلا")),
    CharacterItem(listOf("مؤسس شركة Apple", "رائد ثورة الهواتف الذكية", "iPhone & iPad"), "ستيف جوبز", listOf("بيل غيتس", "ستيف جوبز", "إيلون ماسك")),
    CharacterItem(listOf("أسطورة كرة القدم", "أرجنتيني الأصل", "بطل كأس العالم 2022"), "ميسي", listOf("رونالدو", "نيمار", "ميسي")),
    CharacterItem(listOf("ممثل هوليوود العالمي", "بطل فيلم Titanic", "بطل فيلم Inception"), "ليوناردو دي كابريو", listOf("براد بيت", "ليوناردو دي كابريو", "توم كروز"))
)

@Composable
fun GuessCharacterGameCard(onWin: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(Random.nextInt(CHARACTERS_LIST.size)) }
    var pickedOption by remember { mutableStateOf<String?>(null) }
    val item = CHARACTERS_LIST[currentIndex]

    fun pick(name: String) {
        if (pickedOption != null) return
        pickedOption = name
        if (name == item.name) {
            onWin()
        }
    }

    fun next() {
        currentIndex = Random.nextInt(CHARACTERS_LIST.size)
        pickedOption = null
    }

    GameCardTemplate(
        emoji = "🎭",
        title = "خمن الشخصية",
        desc = "اكتشف الشخصية من التلميحات (+7 نقاط)",
        gradient = listOf(Color(0xFFEC4899), Color(0xFFE11D48))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item.hints.forEach { hint ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "💡 $hint", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.options.forEach { opt ->
                val isCorrect = opt == item.name
                val isPicked = opt == pickedOption
                val bg = when {
                    pickedOption == null -> Color(0xFF1E293B)
                    isCorrect -> PrimaryEmerald
                    isPicked -> ErrorRed
                    else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable(enabled = pickedOption == null) { pick(opt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pickedOption != null && (isCorrect || isPicked)) Color.White else TextPrimary
                    )
                }
            }
        }

        if (pickedOption != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { next() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("شخصية جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// 7. 🌍 خمن الدولة
// ----------------------------------------------------
private val COUNTRIES_LIST = listOf(
    CountryItem("🇯🇵", "اليابان", listOf("الصين", "اليابان", "كوريا")),
    CountryItem("🇫🇷", "فرنسا", listOf("إيطاليا", "ألمانيا", "فرنسا")),
    CountryItem("🇧🇷", "البرازيل", listOf("الأرجنتين", "البرازيل", "المكسيك")),
    CountryItem("🇪🇬", "مصر", listOf("مصر", "السعودية", "المغرب")),
    CountryItem("🇸🇦", "السعودية", listOf("الإمارات", "السعودية", "الكويت")),
    CountryItem("🇮🇹", "إيطاليا", listOf("إسبانيا", "إيطاليا", "اليونان")),
    CountryItem("🇩🇪", "ألمانيا", listOf("ألمانيا", "النمسا", "بولندا"))
)

@Composable
fun GuessCountryGameCard(onWin: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(Random.nextInt(COUNTRIES_LIST.size)) }
    var pickedOption by remember { mutableStateOf<String?>(null) }
    val item = COUNTRIES_LIST[currentIndex]

    fun pick(name: String) {
        if (pickedOption != null) return
        pickedOption = name
        if (name == item.name) {
            onWin()
        }
    }

    fun next() {
        currentIndex = Random.nextInt(COUNTRIES_LIST.size)
        pickedOption = null
    }

    GameCardTemplate(
        emoji = "🌍",
        title = "خمن الدولة",
        desc = "احزر اسم الدولة من علمها (+6 نقاط)",
        gradient = listOf(Color(0xFF10B981), Color(0xFF059669))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.flag, fontSize = 54.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.options.forEach { opt ->
                val isCorrect = opt == item.name
                val isPicked = opt == pickedOption
                val bg = when {
                    pickedOption == null -> Color(0xFF1E293B)
                    isCorrect -> PrimaryEmerald
                    isPicked -> ErrorRed
                    else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable(enabled = pickedOption == null) { pick(opt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pickedOption != null && (isCorrect || isPicked)) Color.White else TextPrimary
                    )
                }
            }
        }

        if (pickedOption != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { next() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("دولة جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// 8. 💰 صندوق الحظ
// ----------------------------------------------------
@Composable
fun LuckyBoxGameCard(onWin: (Int) -> Unit) {
    var openedIndex by remember { mutableStateOf<Int?>(null) }
    var wonPoints by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    val rewards = listOf(3, 5, 8, 10, 15, 20)

    fun open(idx: Int) {
        if (isBusy || openedIndex != null) return
        isBusy = true
        val pts = rewards[Random.nextInt(rewards.size)]
        wonPoints = pts
        openedIndex = idx
        onWin(pts)
        isBusy = false
    }

    fun reset() {
        openedIndex = null
        wonPoints = 0
    }

    GameCardTemplate(
        emoji = "💰",
        title = "صندوق الحظ",
        desc = "افتح صندوقاً واربح نقاطاً عشوائية (3 - 20 نقطة)",
        gradient = listOf(Color(0xFFF59E0B), Color(0xFFE11D48))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            (0..2).forEach { idx ->
                val isOpened = openedIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(85.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isOpened) Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFE11D48)))
                            else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                        )
                        .clickable(enabled = openedIndex == null && !isBusy) { open(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isOpened) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 28.sp)
                            Text("+$wonPoints نقطة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Text("📦", fontSize = 32.sp)
                    }
                }
            }
        }

        if (openedIndex != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { reset() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("صناديق جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// Base Game Card Template
// ----------------------------------------------------
@Composable
fun GameCardTemplate(
    emoji: String,
    title: String,
    desc: String,
    gradient: List<Color>,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column {
            // Header Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradient))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = desc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}
