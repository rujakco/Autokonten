package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize Database and Repository
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context)
                val repository = OtoRepository(database)
                
                // Set up ViewModel factory
                val app = context.applicationContext as Application
                val viewModel: OtoViewModel = viewModel(
                    factory = OtoViewModelFactory(app, repository)
                )

                OtoStudioDashboard(viewModel)
            }
        }
    }
}

@Composable
fun ConsultantTab(viewModel: OtoViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var currentMessageText by remember { mutableStateOf("") }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current

    // Auto scroll to the end on new messages
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expert profile header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StudioGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Pakar",
                        tint = StudioGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "OtoKreator AI Expert Partner 🎓",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioGold
                    )
                    Text(
                        text = "Pakar Marketing, Copywriter Senior & Konsultan Strategi",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pre-defined quick tactical question chips
        Text(
            text = "Konsultasi Cepat:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = StudioAccent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "Review Copywriting" to "Saya ingin Anda me-review copywriting saya untuk produk ini. Apa hook-nya kurang menarik?",
                "Formula Promosi" to "Berikan saya formula promosi diskon/bundling gila-gilaan yang sulit ditolak calon pelanggan!",
                "Struktur Video TikTok" to "Rancang struktur naskah video TikTok berdurasi 30 detik menggunakan formula Hook - Story - Offer!"
            )
            suggestions.forEach { (label, fullQuery) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioCardBg)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isChatLoading) {
                                viewModel.sendChatMessage(fullQuery)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = StudioAccent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Message Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    val isUser = msg.sender == "USER"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .border(
                                    1.dp,
                                    if (isUser) StudioPrimary.copy(alpha = 0.4f) else CardBorderColor,
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 12.dp
                                    )
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) StudioPrimary.copy(alpha = 0.15f) else StudioCardBg
                            ),
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isUser) 12.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 12.dp
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isUser) "Anda" else "Konsultan Ahli AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) StudioAccent else StudioGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                if (isChatLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = StudioGold,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sedang berpikir...", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentMessageText,
                onValueChange = { currentMessageText = it },
                placeholder = { Text("Tanyakan strategi marketing, copywriting...", fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioAccent,
                    unfocusedBorderColor = CardBorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (currentMessageText.isNotBlank() && !isChatLoading) {
                            viewModel.sendChatMessage(currentMessageText)
                            currentMessageText = ""
                        }
                    }
                )
            )

            FloatingActionButton(
                onClick = {
                    if (currentMessageText.isNotBlank() && !isChatLoading) {
                        viewModel.sendChatMessage(currentMessageText)
                        currentMessageText = ""
                    }
                },
                containerColor = StudioPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_chat_message_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Kirim",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtoStudioDashboard(viewModel: OtoViewModel) {
    val context = LocalContext.current
    val currentTab = remember { mutableStateOf(0) }
    
    // Collect Room database states
    val userProducts by viewModel.userProducts.collectAsStateWithLifecycle()
    val trends by viewModel.trends.collectAsStateWithLifecycle()
    val socialPosts by viewModel.socialPosts.collectAsStateWithLifecycle()
    val agentLogs by viewModel.agentLogs.collectAsStateWithLifecycle()
    
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationProgress by viewModel.generationProgress.collectAsStateWithLifecycle()
    val selectedPlatformFilter by viewModel.selectedPlatformFilter.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(StudioPrimary, StudioAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "OtoKreator AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF87))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Semua Agen Siap Beraksi 🤖",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runFullAutomation() },
                        modifier = Modifier
                            .testTag("run_engine_top_button")
                            .clip(CircleShape)
                            .background(StudioPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Jalankan AI",
                            tint = StudioAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioBlack,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioBlack,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab.value == 0,
                    onClick = { currentTab.value = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Pengetahuan") },
                    label = { Text("Aset Data") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioAccent,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = StudioAccent,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = StudioCardBg
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == 1,
                    onClick = { currentTab.value = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Jadwal") },
                    label = { Text("Jadwal") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioAccent,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = StudioAccent,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = StudioCardBg
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == 2,
                    onClick = { currentTab.value = 2 },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Tim Agen") },
                    label = { Text("Tim & Log") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioAccent,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = StudioAccent,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = StudioCardBg
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == 3,
                    onClick = { currentTab.value = 3 },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Pakar AI") },
                    label = { Text("Pakar AI 💬") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioAccent,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = StudioAccent,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = StudioCardBg
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBlack)
        ) {
            // Background ambient lights
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(StudioPrimary.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.8f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(StudioAccent.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width, size.height * 0.7f),
                        radius = size.width * 0.7f
                    )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                when (currentTab.value) {
                    0 -> KnowledgeBaseTab(viewModel, userProducts)
                    1 -> ScheduleTab(viewModel, socialPosts, selectedPlatformFilter)
                    2 -> AgentsLogTab(viewModel, trends, agentLogs)
                    3 -> ConsultantTab(viewModel)
                }
            }

            // Running Engine Loading Overlay
            AnimatedVisibility(
                visible = isGenerating,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioBlack.copy(alpha = 0.92f))
                        .clickable(enabled = false) {}, // absorb clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(24.dp)
                            .border(1.dp, StudioPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = StudioAccent,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Menjalankan Tim Agen",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1 Aplikasi Menggantikan Seluruh Tim Konten",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Chip",
                                        tint = StudioAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = generationProgress,
                                        fontSize = 13.sp,
                                        color = StudioAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Proses ini memakan waktu 10-30 detik karena AI sedang menganalisis tren terkini, menyusun strategi kreatif, dan menulis caption harian Anda secara terintegrasi.",
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// TAB 1: KNOWLEDGE BASE (PRODUCT ASSET LIST)
// ==========================================================
@Composable
fun KnowledgeBaseTab(viewModel: OtoViewModel, assets: List<ProductAsset>) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    val isImportingProducts by viewModel.isImportingProducts.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val text = inputStream.bufferedReader().use { reader -> reader.readText() }
                    rawText = text
                    Toast.makeText(context, "Berhasil memuat file teks! Silakan klik 'Mulai Ekstrak AI' untuk memproses.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var selectedTypeFilter by remember { mutableStateOf("SEMUA") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Manual add asset dialog states
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualTitle by remember { mutableStateOf("") }
    var manualDescription by remember { mutableStateOf("") }
    var manualType by remember { mutableStateOf("PRODUK") }

    val filteredAssets = assets.filter { asset ->
        val matchesType = selectedTypeFilter == "SEMUA" || asset.type == selectedTypeFilter
        val matchesSearch = searchQuery.isBlank() || 
                asset.title.contains(searchQuery, ignoreCase = true) || 
                asset.description.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pangkalan Pengetahuan Konten",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Tempel teks deskripsi produk/jasa, referensi copywriting, atau detail promo Anda di sini. AI akan memilah, mendeteksi tipe aset, dan mengaturnya secara otomatis untuk basis promosi harian.",
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unggah atau Tempel Sumber Data",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioAccent
                        )
                        
                        if (isImportingProducts) {
                            CircularProgressIndicator(
                                color = StudioAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        label = { Text("Materi / Sumber Data Masukan") },
                        placeholder = { 
                            Text("Tempel teks apa saja: deskripsi produk, contoh copywriting iklan, atau info promo diskon gila-gilaan...\n\nAI akan mengekstrak dan mengklasifikasikan tipenya secara otomatis!") 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("product_raw_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = CardBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = StudioAccent
                        ),
                        enabled = !isImportingProducts
                    )
                    
                    if (isImportingProducts) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = StudioGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = importProgress,
                                fontSize = 12.sp,
                                color = StudioGold,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // File picker button
                        Button(
                            onClick = { filePickerLauncher.launch("text/plain") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, StudioAccent, RoundedCornerShape(8.dp))
                                .testTag("upload_txt_file_button"),
                            enabled = !isImportingProducts,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Pilih File", tint = StudioAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pilih File .txt", color = StudioAccent, fontSize = 10.sp)
                        }

                        // Manual Add Button
                        Button(
                            onClick = { showManualAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, StudioGold, RoundedCornerShape(8.dp)),
                            enabled = !isImportingProducts,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Manual", tint = StudioGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah Manual", color = StudioGold, fontSize = 10.sp)
                        }

                        // Process with AI button
                        Button(
                            onClick = {
                                if (rawText.isNotBlank()) {
                                    viewModel.parseAndInsertRawProducts(rawText) { count ->
                                        if (count > 0) {
                                            rawText = ""
                                            Toast.makeText(context, "Berhasil mengimpor & mengekstrak $count aset data! ✨", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Gagal mengimpor produk. Coba periksa teks Anda.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Harap tempel teks atau unggah file .txt terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("add_product_button"),
                            enabled = !isImportingProducts && rawText.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Simpan", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mulai Ekstrak", color = Color.White, fontSize = 10.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Button to load sample if database is empty
                    if (assets.isEmpty()) {
                        TextButton(
                            onClick = { viewModel.setupSampleProduct() },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("load_sample_product_button")
                        ) {
                            Text("Atau muat contoh produk cepat ☕", color = StudioPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari judul atau deskripsi aset...", color = Color.Gray, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioAccent,
                    unfocusedBorderColor = CardBorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = StudioCardBg,
                    unfocusedContainerColor = StudioCardBg
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = StudioAccent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Bersihkan",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Filter Tipe Data Masukan:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "SEMUA" to "Semua Aset 📂",
                    "PRODUK" to "Produk 📦",
                    "COPYWRITING" to "Copywriting 📝",
                    "PROMO" to "Promo 🏷️"
                ).forEach { (type, label) ->
                    val isSelected = selectedTypeFilter == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) StudioPrimary else StudioCardBg)
                            .border(1.dp, if (isSelected) StudioAccent else CardBorderColor, RoundedCornerShape(20.dp))
                            .clickable { selectedTypeFilter = type }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pangkalan Pengetahuan (${filteredAssets.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (filteredAssets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Kosong",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada aset data tipe $selectedTypeFilter yang disimpan.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Materi bisa berupa info produk, diskon, atau copywriting.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(filteredAssets) { asset ->
                ProductAssetCard(asset, onDelete = { viewModel.deleteProduct(asset.id) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showManualAddDialog) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            containerColor = StudioCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tambah Aset Manual 📦",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text("Nama/Judul Aset", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = manualDescription,
                        onValueChange = { manualDescription = it },
                        label = { Text("Deskripsi / Detail Konten", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    
                    Text("Tipe Aset:", fontSize = 11.sp, color = StudioAccent, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PRODUK" to "Produk 📦",
                            "COPYWRITING" to "Copywriting 📝",
                            "PROMO" to "Promo 🏷️"
                        ).forEach { (type, label) ->
                            val isSelected = manualType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) StudioPrimary else Color.Transparent)
                                    .border(1.dp, if (isSelected) StudioAccent else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable { manualType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualTitle.isNotBlank() && manualDescription.isNotBlank()) {
                            viewModel.addManualProduct(manualTitle, manualDescription, manualType)
                            showManualAddDialog = false
                            manualTitle = ""
                            manualDescription = ""
                            Toast.makeText(context, "Aset berhasil ditambahkan secara manual! ✨", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Harap lengkapi semua bidang!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Simpan Aset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ProductAssetCard(asset: ProductAsset, onDelete: () -> Unit) {
    val badgeColor = when (asset.type) {
        "PRODUK" -> Color(0xFFC084FC) // Purple
        "COPYWRITING" -> Color(0xFF60A5FA) // Blue
        "PROMO" -> Color(0xFFFBBF24) // Gold
        else -> StudioAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = asset.type,
                            fontSize = 8.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = asset.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = asset.description,
                fontSize = 13.sp,
                color = Color.LightGray,
                lineHeight = 18.sp
            )
        }
    }
}

// ==========================================================
// ==========================================================
// TAB 2: SCHEDULE & CALENDAR VIEW
// ==========================================================
@Composable
fun ScheduleTab(
    viewModel: OtoViewModel,
    posts: List<SocialPost>,
    platformFilter: String
) {
    val context = LocalContext.current
    val filteredPosts = posts.filter { it.platform == platformFilter }
    val isRegeneratingMap by viewModel.isRegeneratingMap.collectAsStateWithLifecycle()
    val weeklyCampaign by viewModel.weeklyCampaign.collectAsStateWithLifecycle()
    val isGeneratingWeeklyCampaign by viewModel.isGeneratingWeeklyCampaign.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kalender & Jadwal Konten",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Jadwal otomatis 5 postingan harian relate dengan waktu puncak.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
                
                Button(
                    onClick = { viewModel.runFullAutomation() },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("trigger_automation_tab_button")
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Buat Konten", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buat Konten", color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Weekly Campaign Theme Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, StudioAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Campaign Theme",
                                tint = StudioGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tema Kampanye Mingguan 🎯",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioGold
                            )
                        }
                        
                        if (isGeneratingWeeklyCampaign) {
                            CircularProgressIndicator(
                                color = StudioGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            TextButton(
                                onClick = { viewModel.generateWeeklyCampaign() },
                                colors = ButtonDefaults.textButtonColors(contentColor = StudioAccent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (weeklyCampaign.isBlank()) "Rancang AI 🚀" else "Rancang Ulang 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (weeklyCampaign.isBlank()) {
                        Text(
                            text = "Belum ada rencana tema kampanye mingguan. Tekan 'Rancang AI' untuk menganalisis pangkalan data produk Anda dan menyusun rencana tema kampanye berurutan selama 4 minggu ke depan secara otomatis!",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = weeklyCampaign,
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Tema Kampanye", weeklyCampaign)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Tema kampanye disalin ke papan klip! 📋", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = StudioAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin Rencana 📋", fontSize = 10.sp, color = StudioAccent)
                            }
                        }
                    }
                }
            }
        }

        // Platform Filter Selection Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCardBg)
                    .padding(4.dp)
            ) {
                listOf("TIKTOK", "INSTAGRAM", "THREADS").forEach { platform ->
                    val isSelected = platformFilter == platform
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) StudioPrimary else Color.Transparent)
                            .clickable { viewModel.setPlatformFilter(platform) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = platform,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Jadwal Kosong",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Jadwal hari ini masih kosong.",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Klik 'Buat Konten' di atas untuk menyuruh Agen AI merancang 15 konten sosial media harian secara otomatis!",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredPosts) { post ->
                val isRegen = isRegeneratingMap[post.id] ?: false
                SocialPostCard(
                    post = post,
                    onPublish = { viewModel.publishPostNow(context, post) },
                    onDelete = { viewModel.deleteSocialPost(post.id) },
                    onUpdate = { viewModel.updateSocialPost(it) },
                    onRegenerate = { p, instruction, onDone ->
                        viewModel.regenerateSocialPost(p, instruction, onDone)
                    },
                    isRegenerating = isRegen,
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SocialPostCard(
    post: SocialPost, 
    onPublish: () -> Unit, 
    onDelete: () -> Unit,
    onUpdate: (SocialPost) -> Unit,
    onRegenerate: (SocialPost, String, (Boolean) -> Unit) -> Unit,
    isRegenerating: Boolean,
    viewModel: OtoViewModel
) {
    val brandColor = when (post.platform) {
        "TIKTOK" -> BrandTikTok
        "INSTAGRAM" -> BrandInstagram
        else -> BrandThreadsBorder
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editConcept by remember { mutableStateOf(post.creativeIdea) }
    var editCaption by remember { mutableStateOf(post.caption) }
    var editHashtags by remember { mutableStateOf(post.hashtags) }
    var editPrompt by remember { mutableStateOf(post.promptUsed) }

    var showRegenDialog by remember { mutableStateOf(false) }
    var customInstruction by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isAlarmActive by remember { mutableStateOf(viewModel.isAlarmActive(context, post.id)) }

    var showAlarmTimeDialog by remember { mutableStateOf(false) }
    var selectedAlarmHour by remember { mutableStateOf(post.scheduleHour) }
    var selectedAlarmMinute by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAlarmTimeDialog = true
        } else {
            Toast.makeText(context, "Izin notifikasi ditolak. Pengingat tidak dapat ditampilkan.", Toast.LENGTH_LONG).show()
        }
    }

    // Sync state if post changes externally (e.g. after regeneration)
    LaunchedEffect(post) {
        editConcept = post.creativeIdea
        editCaption = post.caption
        editHashtags = post.hashtags
        editPrompt = post.promptUsed
        isAlarmActive = viewModel.isAlarmActive(context, post.id)
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = StudioCardBg,
            title = {
                Text(
                    text = "Edit Konten Media Sosial ✏️",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editConcept,
                        onValueChange = { editConcept = it },
                        label = { Text("Konsep Video/Visual", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCaption,
                        onValueChange = { editCaption = it },
                        label = { Text("Copywriting Caption", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = editHashtags,
                        onValueChange = { editHashtags = it },
                        label = { Text("Hashtags", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrompt,
                        onValueChange = { editPrompt = it },
                        label = { Text("Visual Prompt (Inggris)", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = post.copy(
                            creativeIdea = editConcept,
                            caption = editCaption,
                            hashtags = editHashtags,
                            promptUsed = editPrompt
                        )
                        onUpdate(updated)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    if (showRegenDialog) {
        AlertDialog(
            onDismissRequest = { showRegenDialog = false },
            containerColor = StudioCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = StudioGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tulis Ulang AI ✨",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Beri tahu Agen AI apa yang ingin Anda ubah atau sesuaikan (misal: 'buat lebih lucu', 'fokuskan ke promo diskon 20%', 'buat lebih persuasif').",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customInstruction,
                        onValueChange = { customInstruction = it },
                        placeholder = { Text("Masukkan instruksi kustom di sini...", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customInstruction.isNotBlank()) {
                            onRegenerate(post, customInstruction) { success ->
                                // Optional callback completion
                            }
                            showRegenDialog = false
                            customInstruction = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccent)
                ) {
                    Text("Proses Ulang 🚀", color = StudioBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    if (showAlarmTimeDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmTimeDialog = false },
            containerColor = StudioCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = StudioGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Setel Pengingat Kustom ⏰",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Atur waktu pengingat khusus untuk slot ${post.slotName} agar notifikasi muncul sesuai preferensi Anda.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    
                    // Hour selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jam:", fontSize = 13.sp, color = StudioAccent, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (selectedAlarmHour > 0) selectedAlarmHour-- else selectedAlarmHour = 23 }) {
                                    Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedAlarmHour.toString().padStart(2, '0'), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(onClick = { if (selectedAlarmHour < 23) selectedAlarmHour++ else selectedAlarmHour = 0 }) {
                                    Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Slider(
                            value = selectedAlarmHour.toFloat(),
                            onValueChange = { selectedAlarmHour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioAccent,
                                activeTrackColor = StudioAccent,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }

                    // Minute selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Menit:", fontSize = 13.sp, color = StudioAccent, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (selectedAlarmMinute > 0) selectedAlarmMinute = (selectedAlarmMinute - 5 + 60) % 60 else selectedAlarmMinute = 55 }) {
                                    Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedAlarmMinute.toString().padStart(2, '0'), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(onClick = { selectedAlarmMinute = (selectedAlarmMinute + 5) % 60 }) {
                                    Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Slider(
                            value = selectedAlarmMinute.toFloat(),
                            onValueChange = { selectedAlarmMinute = (it.toInt() / 5) * 5 },
                            valueRange = 0f..55f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioAccent,
                                activeTrackColor = StudioAccent,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }

                    // Quick buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Jadwal Asli" to {
                                selectedAlarmHour = post.scheduleHour
                                selectedAlarmMinute = 0
                            },
                            "Siang (12:00)" to {
                                selectedAlarmHour = 12
                                selectedAlarmMinute = 0
                            },
                            "Malam (19:30)" to {
                                selectedAlarmHour = 19
                                selectedAlarmMinute = 30
                            }
                        ).forEach { (label, action) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioCardBg.copy(alpha = 0.5f))
                                    .border(0.5.dp, Color.Gray, RoundedCornerShape(6.dp))
                                    .clickable { action() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 9.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.schedulePostAlarm(context, post, selectedAlarmHour, selectedAlarmMinute)
                        isAlarmActive = true
                        showAlarmTimeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("Aktifkan Alarm 🔔", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmTimeDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Slot Time & Platform badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(brandColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = post.platform,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (post.platform == "THREADS") Color.White else brandColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = post.slotName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val prefs = remember(isAlarmActive) { context.getSharedPreferences("otokreator_prefs", Context.MODE_PRIVATE) }
                    val alarmHour = remember(isAlarmActive) { prefs.getInt("alarm_time_hour_${post.id}", post.scheduleHour) }
                    val alarmMinute = remember(isAlarmActive) { prefs.getInt("alarm_time_minute_${post.id}", 0) }
                    val timeString = if (isAlarmActive) {
                        "${alarmHour.toString().padStart(2, '0')}:${alarmMinute.toString().padStart(2, '0')} 🔔"
                    } else {
                        "${post.scheduleHour.toString().padStart(2, '0')}:00"
                    }
                    Text(
                        text = "($timeString)",
                        fontSize = 12.sp,
                        color = if (isAlarmActive) StudioGold else Color.Gray,
                        fontWeight = if (isAlarmActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (post.status == "PUBLISHED") Color(0xFF00FF87).copy(alpha = 0.2f) else StudioGold.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (post.status == "PUBLISHED") "TERPUBLIKASI ✅" else "TERJADWAL ⏰",
                            fontSize = 9.sp,
                            color = if (post.status == "PUBLISHED") Color(0xFF00FF87) else StudioGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (isAlarmActive) {
                                viewModel.cancelPostAlarm(context, post)
                                isAlarmActive = false
                            } else {
                                if (android.os.Build.VERSION.SDK_INT >= 33) {
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        "android.permission.POST_NOTIFICATIONS"
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasPermission) {
                                        showAlarmTimeDialog = true
                                    } else {
                                        permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                    }
                                } else {
                                    showAlarmTimeDialog = true
                                }
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Setel Alarm",
                            tint = if (isAlarmActive) StudioGold else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Creative Concept
            Text(
                text = "Konsep Video/Visual:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioAccent
            )
            Text(
                text = post.creativeIdea,
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Dynamic Generated Image Mockup Card (Absolute Craft / Aesthetic Showcase)
            Text(
                text = "Simulasi Aset Gambar/Storyboard Video:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            ContentMockThumbnail(post = post)
            Spacer(modifier = Modifier.height(12.dp))

            // Caption Box
            Text(
                text = "Salinan Copywriting & Hashtags:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(0.5.dp, CardBorderColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = post.caption,
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post.hashtags,
                        fontSize = 11.sp,
                        color = StudioAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Caption", "${post.caption}\n\n${post.hashtags}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Caption & Hashtag disalin! 📝", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = "Salin Caption", tint = StudioAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin Caption 📝", color = StudioAccent, fontSize = 9.sp)
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Visual Prompt", post.promptUsed)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Visual Prompt disalin! 🎨", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Salin Prompt", tint = StudioGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin Prompt 🎨", color = StudioGold, fontSize = 9.sp)
                }

                Button(
                    onClick = {
                        try {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${post.caption}\n\n${post.hashtags}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan copywriting ke..."))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membagikan konten: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan Konten 📲", color = Color.White, fontSize = 9.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Actions Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = StudioAccent),
                        enabled = !isRegenerating
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = { showRegenDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = StudioGold),
                        enabled = !isRegenerating
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Tulis Ulang", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tulis Ulang AI", fontSize = 11.sp)
                    }
                }

                if (isRegenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = StudioGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memproses...", fontSize = 11.sp, color = StudioGold)
                    }
                } else {
                    Button(
                        onClick = onPublish,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (post.status == "PUBLISHED") Icons.Default.Check else Icons.Default.Share,
                            contentDescription = "Publish",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (post.status == "PUBLISHED") "Publikasi Lagi" else "Salin & Posting",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================
// BEAUTIFUL CUSTOM CANVAS DRAWING FOR SOCIAL ASSET PREVIEWS
// ==========================================================
@Composable
fun ContentMockThumbnail(post: SocialPost) {
    val platformColor = when (post.platform) {
        "TIKTOK" -> BrandTikTok
        "INSTAGRAM" -> BrandInstagram
        else -> Color.DarkGray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(platformColor.copy(alpha = 0.35f), StudioBlack),
                    radius = 350f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative grid line vectors
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 20.dp.toPx()
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (post.platform) {
                    "TIKTOK" -> Icons.Default.PlayArrow
                    "INSTAGRAM" -> Icons.Default.Star
                    else -> Icons.Default.Info
                },
                contentDescription = "Brief Visual",
                tint = StudioAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PROMPT GENERATOR:",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = StudioGold
            )
            Text(
                text = post.promptUsed,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (post.platform == "TIKTOK") "Video Storyboard Ready 🎬" else "Image Generation Prompt Ready 🖼️",
                    fontSize = 9.sp,
                    color = StudioAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================================
// TAB 3: AGENTS CREW STATUS & LOGGER
// ==========================================================
@Composable
fun AgentsLogTab(
    viewModel: OtoViewModel,
    trends: List<TrendTopic>,
    logs: List<AgentLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Status Tim Agen AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Semua proses ideasi, riset tren, penulisan konten harian dipecah dan dieksekusi secara berurutan oleh kru agen AI khusus.",
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }

        // Horizontal status grid of the 4 Agents
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Struktur Kru Agen Terintegrasi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AgentRowItem(name = "1. Trend Finder Agent 🔍", desc = "Mencari hot topik di media sosial & news", status = "SIAP BEKERJA")
                    Divider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    AgentRowItem(name = "2. Creative Brainstormer 💡", desc = "Mengkombinasikan produk dengan tren viral", status = "SIAP BEKERJA")
                    Divider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    AgentRowItem(name = "3. Content Planner Agent 📅", desc = "Menyusun jadwal optimal relate waktu upload", status = "SIAP BEKERJA")
                    Divider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    AgentRowItem(name = "4. Media & Prompt Engineer 🎨", desc = "Menulis copywriting caption & prompt visual", status = "SIAP BEKERJA")
                    Divider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    AgentRowItem(name = "5. Auto-Publisher Agent 🚀", desc = "Mengatur distribusi dan simulasi post langsung", status = "AKTIF")
                }
            }
        }

        // Live Discovered Trends section
        item {
            var showAddTrend by remember { mutableStateOf(false) }
            var manualTrendTitle by remember { mutableStateOf("") }
            var manualTrendSource by remember { mutableStateOf("") }
            var manualTrendHotness by remember { mutableStateOf("Tinggi 🔥") }
            var manualTrendSummary by remember { mutableStateOf("") }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Terkini Yang Ditemukan (${trends.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                TextButton(
                    onClick = { showAddTrend = !showAddTrend },
                    colors = ButtonDefaults.textButtonColors(contentColor = StudioAccent)
                ) {
                    Icon(
                        imageVector = if (showAddTrend) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Toggle Tambah Tren",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showAddTrend) "Tutup" else "Tambah Tren", fontSize = 12.sp)
                }
            }

            AnimatedVisibility(visible = showAddTrend) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, StudioAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tambah Tren Kustom Manual ➕",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioAccent
                        )
                        Text(
                            text = "Konteks tren kustom ini akan langsung digunakan Agen AI untuk mengonsep konten kreatif.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = manualTrendTitle,
                            onValueChange = { manualTrendTitle = it },
                            label = { Text("Judul Tren / Isu Viral", color = Color.Gray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioAccent,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualTrendSource,
                                onValueChange = { manualTrendSource = it },
                                label = { Text("Sumber (Contoh: TikTok)", color = Color.Gray, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioAccent,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1.5f)
                            )

                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Skala Tren", fontSize = 10.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Tinggi 🔥", "Sedang ⚡", "Normal 📌").forEach { hotnessVal ->
                                        val isSelected = manualTrendHotness == hotnessVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) StudioPrimary else Color.Black.copy(alpha = 0.4f))
                                                .clickable { manualTrendHotness = hotnessVal }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = hotnessVal.split(" ")[0], // Show visual emoji/short
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = manualTrendSummary,
                            onValueChange = { manualTrendSummary = it },
                            label = { Text("Ringkasan Isu / Konteks Viral", color = Color.Gray, fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioAccent,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (manualTrendTitle.isNotBlank() && manualTrendSummary.isNotBlank()) {
                                    viewModel.insertManualTrend(
                                        title = manualTrendTitle,
                                        source = manualTrendSource,
                                        hotness = manualTrendHotness,
                                        summary = manualTrendSummary
                                    )
                                    // Clear inputs
                                    manualTrendTitle = ""
                                    manualTrendSource = ""
                                    manualTrendSummary = ""
                                    showAddTrend = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioAccent),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Tren Kustom", color = StudioBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (trends.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada tren yang discan. Jalankan mesin otomatis untuk mulai memindai internet!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(trends) { trend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(0.5.dp, CardBorderColor, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trend.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StudioPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = trend.source,
                                    fontSize = 9.sp,
                                    color = StudioAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = trend.summary,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Live Action Logs list
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktivitas & Log Eksekusi Agen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada log aktivitas. Mulai jalankan tim agen!",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                LogItemRow(log)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AgentRowItem(name: String, desc: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = desc, fontSize = 11.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (status == "AKTIF") Color(0xFF00FF87).copy(alpha = 0.2f) else StudioAccent.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                fontSize = 9.sp,
                color = if (status == "AKTIF") Color(0xFF00FF87) else StudioAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LogItemRow(log: AgentLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudioBlack),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .drawBehind {
                    drawLine(
                        color = StudioPrimary.copy(alpha = 0.3f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 4f
                    )
                }
                .padding(start = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.agentName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioAccent
                )
                Text(
                    text = "Aktivitas Terlacak",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = log.action,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (log.details.isNotEmpty()) {
                Text(
                    text = log.details,
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}