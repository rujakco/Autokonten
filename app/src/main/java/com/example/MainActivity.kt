package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                    label = { Text("Aset Produk") },
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
                    label = { Text("Jadwal Konten") },
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
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val listState = remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pangkalan Pengetahuan Produk",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Masukkan deskripsi atau copy-paste data produk Anda. AI akan menyerap data ini sebagai basis pengetahuan untuk seluruh konten harian.",
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
                    Text(
                        text = "Input Informasi Produk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Produk / Jasa") },
                        placeholder = { Text("Contoh: Kopi Susu Gula Aren Botolan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = CardBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = StudioAccent
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Detail / Fitur / Harga / Nilai Jual") },
                        placeholder = { Text("Contoh: Premium quality Gula Aren, terbuat dari 100% Arabika Gayo, tanpa pengawet, botol 1 liter...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("product_description_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = CardBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = StudioAccent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.setupSampleProduct() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .border(1.dp, StudioPrimary, RoundedCornerShape(8.dp))
                                .testTag("load_sample_product_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Muat Contoh Kopi ☕", color = StudioPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank() && description.isNotBlank()) {
                                    viewModel.insertProduct(title, description)
                                    title = ""
                                    description = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("add_product_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Simpan", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Produk", color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pengetahuan Terdaftar (${assets.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (assets.isEmpty()) {
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
                            text = "Belum ada produk yang disimpan.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Klik 'Muat Contoh Kopi' untuk mencoba instan!",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(assets) { asset ->
                ProductAssetCard(asset, onDelete = { viewModel.deleteProduct(asset.id) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProductAssetCard(asset: ProductAsset, onDelete: () -> Unit) {
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
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Aset",
                        tint = StudioAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = asset.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.75f)
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
                    isRegenerating = isRegen
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
    isRegenerating: Boolean
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

    // Sync state if post changes externally (e.g. after regeneration)
    LaunchedEffect(post) {
        editConcept = post.creativeIdea
        editCaption = post.caption
        editHashtags = post.hashtags
        editPrompt = post.promptUsed
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
                    Text(
                        text = "(${post.scheduleHour.toString().padStart(2, '0')}:00)",
                        fontSize = 12.sp,
                        color = Color.Gray
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
