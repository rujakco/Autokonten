package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.AgentOrchestrator
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OtoViewModel(application: Application, private val repository: OtoRepository) : AndroidViewModel(application) {

    // Main States
    val userProducts: StateFlow<List<ProductAsset>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trends: StateFlow<List<TrendTopic>> = repository.allTrends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val socialPosts: StateFlow<List<SocialPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agentLogs: StateFlow<List<AgentLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Operation States
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow("")
    val generationProgress: StateFlow<String> = _generationProgress.asStateFlow()

    private val _selectedPlatformFilter = MutableStateFlow("TIKTOK")
    val selectedPlatformFilter: StateFlow<String> = _selectedPlatformFilter.asStateFlow()

    fun setPlatformFilter(platform: String) {
        _selectedPlatformFilter.value = platform
    }

    // Insert Product Asset
    fun insertProduct(title: String, description: String, imageUri: String? = null) {
        viewModelScope.launch {
            if (title.isBlank() || description.isBlank()) return@launch
            repository.insertAsset(ProductAsset(title = title, description = description, imageUri = imageUri))
            repository.insertLog(AgentLog("System", "Produk baru ditambahkan ke basis pengetahuan.", "Judul: $title"))
        }
    }

    // Delete Product Asset
    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteAssetById(id)
            repository.insertLog(AgentLog("System", "Menghapus produk dari basis pengetahuan.", "ID: $id"))
        }
    }

    // Run AI Orchestrator Chain
    fun runFullAutomation() {
        if (_isGenerating.value) return
        viewModelScope.launch {
            _isGenerating.value = true
            _generationProgress.value = "Menghubungi tim agen otomatis..."
            
            val success = AgentOrchestrator.runAutomationChain(
                repository = repository,
                userProducts = userProducts.value,
                onProgressUpdate = { progress ->
                    _generationProgress.value = progress
                }
            )
            
            _isGenerating.value = false
            _generationProgress.value = ""
        }
    }

    // Publish Post now (Simulate)
    fun publishPostNow(context: Context, post: SocialPost) {
        viewModelScope.launch {
            // 1. Copy caption & hashtags to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val fullText = "${post.caption}\n\n${post.hashtags}"
            val clip = ClipData.newPlainText("Social Post Caption", fullText)
            clipboard.setPrimaryClip(clip)

            // 2. Update post status to published
            repository.updatePostStatus(post.id, "PUBLISHED")

            // 3. Log publishing activity
            repository.insertLog(
                AgentLog(
                    agentName = "Auto-Publisher",
                    action = "Memposting konten langsung ke ${post.platform}.",
                    details = "Slot: ${post.slotName}, Jam: ${post.scheduleHour}:00.\nCaption berhasil disalin ke papan klip!"
                )
            )

            Toast.makeText(context, "Caption disalin ke Clipboard! Siap dipos ke ${post.platform}.", Toast.LENGTH_LONG).show()
        }
    }

    // Delete social post from list
    fun deleteSocialPost(id: Int) {
        viewModelScope.launch {
            repository.deletePostById(id)
        }
    }

    // Manual update of social post
    fun updateSocialPost(post: SocialPost) {
        viewModelScope.launch {
            repository.insertPost(post)
            repository.insertLog(
                AgentLog(
                    agentName = "Editor Konten ✏️",
                    action = "Mengedit postingan secara manual.",
                    details = "Platform: ${post.platform}, Slot: ${post.slotName}, Jam: ${post.scheduleHour}:00."
                )
            )
        }
    }

    // Individual post regeneration states and handler
    private val _isRegeneratingMap = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isRegeneratingMap: StateFlow<Map<Int, Boolean>> = _isRegeneratingMap.asStateFlow()

    fun regenerateSocialPost(post: SocialPost, instruction: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isRegeneratingMap.update { it + (post.id to true) }
            repository.insertLog(AgentLog("Media & Prompt Engineer 🎨", "Memulai pembuatan ulang konten kustom via AI.", "Platform: ${post.platform}, Permintaan: \"$instruction\""))
            
            val systemInstruction = """
                Anda adalah Media & Prompt Engineer Agent handal. Tugas Anda adalah memodifikasi dan mengoptimalkan satu postingan sosial media spesifik sesuai dengan instruksi perubahan dari pengguna.
                
                Pertahankan platform: ${post.platform}, slot: ${post.slotName}, dan jam: ${post.scheduleHour}.
                
                Instruksi Perubahan dari Pengguna:
                $instruction
                
                Data Postingan Saat Ini:
                Konsep: ${post.creativeIdea}
                Caption: ${post.caption}
                Hashtags: ${post.hashtags}
                VisualPrompt: ${post.promptUsed}
                
                Tuliskan konten baru dalam format tag [POST] ... [END_POST] berikut.
                Patuhi format ini secara ketat agar aplikasi kami bisa mem-parsingnya dengan sukses!
                
                [POST]
                Platform: ${post.platform}
                Slot: ${post.slotName}
                Jam: ${post.scheduleHour}
                Konsep: <Penjelasan ide video/visual baru yang disesuaikan>
                Caption: <Caption baru gaya lokal Indonesia, sesuaikan dengan instruksi pengguna>
                Hashtags: <Hashtag dipisahkan spasi>
                VisualPrompt: <Prompt detail baru dalam bahasa inggris untuk membuat gambar AI>
                [END_POST]
            """.trimIndent()

            try {
                val resultText = com.example.api.GeminiClient.generateContent(
                    prompt = "Buat ulang postingan ini sesuai instruksi.",
                    systemInstruction = systemInstruction
                )
                
                if (resultText.contains("[POST]") && resultText.contains("[END_POST]")) {
                    val block = resultText.substringAfter("[POST]").substringBefore("[END_POST]")
                    var konsep = post.creativeIdea
                    var caption = post.caption
                    var hashtags = post.hashtags
                    var visualPrompt = post.promptUsed
                    
                    val lines = block.trim().lines()
                    var currentField = ""
                    val fieldBuilder = StringBuilder()
                    
                    for (line in lines) {
                        val trimmedLine = line.trim()
                        when {
                            trimmedLine.startsWith("Konsep:") -> {
                                konsep = trimmedLine.substringAfter("Konsep:").trim()
                            }
                            trimmedLine.startsWith("Caption:") -> {
                                currentField = "caption"
                                fieldBuilder.clear()
                                fieldBuilder.append(trimmedLine.substringAfter("Caption:").trim())
                            }
                            trimmedLine.startsWith("Hashtags:") -> {
                                if (currentField == "caption") {
                                    caption = fieldBuilder.toString().trim()
                                }
                                currentField = "hashtags"
                                fieldBuilder.clear()
                                fieldBuilder.append(trimmedLine.substringAfter("Hashtags:").trim())
                            }
                            trimmedLine.startsWith("VisualPrompt:") -> {
                                if (currentField == "hashtags") {
                                    hashtags = fieldBuilder.toString().trim()
                                } else if (currentField == "caption") {
                                    caption = fieldBuilder.toString().trim()
                                }
                                currentField = "visual"
                                fieldBuilder.clear()
                                fieldBuilder.append(trimmedLine.substringAfter("VisualPrompt:").trim())
                            }
                            else -> {
                                if (currentField.isNotEmpty()) {
                                    fieldBuilder.append("\n").append(line)
                                }
                            }
                        }
                    }
                    
                    if (currentField == "visual") {
                        visualPrompt = fieldBuilder.toString().trim()
                    } else if (currentField == "hashtags") {
                        hashtags = fieldBuilder.toString().trim()
                    } else if (currentField == "caption") {
                        caption = fieldBuilder.toString().trim()
                    }
                    
                    val updatedPost = post.copy(
                        creativeIdea = konsep,
                        caption = caption,
                        hashtags = hashtags,
                        promptUsed = visualPrompt
                    )
                    
                    repository.insertPost(updatedPost)
                    repository.insertLog(AgentLog("Media & Prompt Engineer 🎨", "Sukses memperbarui konten via AI sesuai instruksi Anda.", "Platform: ${post.platform}, Slot: ${post.slotName}"))
                    onComplete(true)
                } else {
                    repository.insertLog(AgentLog("Media & Prompt Engineer 🎨", "Gagal parsing format regenerasi AI.", "Format respons tidak mengandung tag POST yang valid."))
                    onComplete(false)
                }
            } catch (e: Exception) {
                repository.insertLog(AgentLog("Media & Prompt Engineer 🎨", "Kesalahan saat memproses regenerasi.", e.message ?: "Unknown Error"))
                onComplete(false)
            } finally {
                _isRegeneratingMap.update { it + (post.id to false) }
            }
        }
    }

    // Insert Manual Trend Topic
    fun insertManualTrend(title: String, source: String, hotness: String, summary: String) {
        viewModelScope.launch {
            if (title.isBlank() || summary.isBlank()) return@launch
            val newTrend = TrendTopic(
                title = title,
                source = if (source.isBlank()) "Tren Manual" else source,
                hotness = hotness,
                summary = summary
            )
            repository.insertTrend(newTrend)
            repository.insertLog(
                AgentLog(
                    agentName = "Trend Finder 🔍",
                    action = "Menambahkan tren kustom secara manual.",
                    details = "Judul: $title, Sumber: $source, Hotness: $hotness."
                )
            )
        }
    }

    // Clear logs
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Setup dummy product for ease of use on first run
    fun setupSampleProduct() {
        viewModelScope.launch {
            val sample = ProductAsset(
                title = "OtoBrew Coffee Milk 1L",
                description = "Kopi Susu Gula Aren premium kemasan botol kaca 1 liter. Dibuat menggunakan 100% biji kopi Arabika Gayo fresh brew, susu murni rendah lemak, dan pemanis gula aren cair organik dari petani lokal. Nikmat, praktis, dan tahan 5 hari di kulkas."
            )
            repository.insertAsset(sample)
            repository.insertLog(AgentLog("System", "Menambahkan produk sampel.", "OtoBrew Coffee Milk 1L"))
        }
    }
}

class OtoViewModelFactory(
    private val application: Application,
    private val repository: OtoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtoViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
