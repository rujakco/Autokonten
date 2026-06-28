package com.example.ui

import android.app.Application
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ContentReminderReceiver
import com.example.api.AgentOrchestrator
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private val _isImportingProducts = MutableStateFlow(false)
    val isImportingProducts: StateFlow<Boolean> = _isImportingProducts.asStateFlow()

    private val _importProgress = MutableStateFlow("")
    val importProgress: StateFlow<String> = _importProgress.asStateFlow()

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

    // Parse and extract multiple products using Gemini API
    fun parseAndInsertRawProducts(rawText: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val trimmedText = rawText.trim()
            if (trimmedText.isBlank()) {
                onComplete(0)
                return@launch
            }
            _isImportingProducts.value = true
            _importProgress.value = "AI sedang menganalisis dan mengekstrak masukan..."
            repository.insertLog(AgentLog("Content Extractor 🤖", "Mulai memproses data masukan mentah.", "Panjang: ${trimmedText.length} karakter."))

            val systemInstruction = """
                Anda adalah AI Asset Extraction Agent yang handal. Tugas Anda adalah membaca, menganalisis, dan mengekstrak rincian informasi dari teks mentah/acak (seperti hasil copy-paste website, rincian produk, catatan copywriting, atau detail promosi/diskon).
                Anda dapat mengekstrak satu atau beberapa item sekaligus jika teks berisi rincian yang berbeda.
                
                Klasifikasikan setiap item ke dalam salah satu dari 3 tipe berikut secara tepat:
                - PRODUK: Untuk spesifikasi barang, katalog, jasa, atau deskripsi produk.
                - COPYWRITING: Untuk contoh/referensi tulisan iklan, caption inspirasional, slogan, hook.
                - PROMO: Untuk info diskon, event musiman, voucher, bundling harga, kupon.
                
                Tuliskan setiap item yang berhasil diekstrak dalam format tag [PRODUCT] ... [END_PRODUCT] berikut secara ketat:
                
                [PRODUCT]
                Tipe: <Isi dengan salah satu tipe di atas secara ketat: PRODUK atau COPYWRITING atau PROMO>
                Nama: <Nama singkat produk, judul copywriting, atau judul promo/event>
                Deskripsi: <Deskripsi detail produk, tulisan copywriting lengkap, atau ketentuan promo/diskon>
                [END_PRODUCT]
                
                Pastikan Anda hanya menggunakan data asli dari teks yang diberikan. Jangan mengada-ada informasi yang tidak ada dalam teks.
            """.trimIndent()

            try {
                val resultText = com.example.api.GeminiClient.generateContent(
                    prompt = trimmedText,
                    systemInstruction = systemInstruction
                )

                var count = 0
                if (resultText.contains("[PRODUCT]") && resultText.contains("[END_PRODUCT]")) {
                    val blocks = resultText.split("[PRODUCT]")
                    for (block in blocks) {
                        if (!block.contains("[END_PRODUCT]")) continue
                        val content = block.substringBefore("[END_PRODUCT]").trim()
                        var tipe = "PRODUK"
                        var nama = ""
                        var deskripsi = ""

                        val lines = content.lines()
                        var currentField = ""
                        val descBuilder = StringBuilder()

                        for (line in lines) {
                            val trimmedLine = line.trim()
                            if (trimmedLine.startsWith("Tipe:")) {
                                val parsedType = trimmedLine.substringAfter("Tipe:").trim().uppercase()
                                if (parsedType == "PRODUK" || parsedType == "COPYWRITING" || parsedType == "PROMO") {
                                    tipe = parsedType
                                }
                            } else if (trimmedLine.startsWith("Nama:")) {
                                nama = trimmedLine.substringAfter("Nama:").trim()
                            } else if (trimmedLine.startsWith("Deskripsi:")) {
                                currentField = "deskripsi"
                                descBuilder.clear()
                                descBuilder.append(trimmedLine.substringAfter("Deskripsi:").trim())
                            } else {
                                if (currentField == "deskripsi") {
                                    descBuilder.append("\n").append(line)
                                }
                            }
                        }

                        if (currentField == "deskripsi") {
                            deskripsi = descBuilder.toString().trim()
                        }

                        if (nama.isNotBlank() && deskripsi.isNotBlank()) {
                            repository.insertAsset(ProductAsset(title = nama, description = deskripsi, type = tipe))
                            repository.insertLog(AgentLog("Content Extractor 🤖", "Berhasil mengekstrak item dari teks mentah.", "Tipe: $tipe, Judul: $nama"))
                            count++
                        }
                    }
                }

                if (count > 0) {
                    repository.insertLog(AgentLog("System", "Selesai memproses impor batch aset.", "$count aset berhasil diklasifikasikan dan ditambahkan."))
                    onComplete(count)
                } else {
                    // Fallback to single product parsing if AI failed to format or returned unparseable text
                    _importProgress.value = "Format khusus tidak terdeteksi, mengimpor sebagai satu produk tunggal..."
                    val firstLine = trimmedText.lines().firstOrNull()?.take(50) ?: "Aset Impor Tanpa Nama"
                    val title = firstLine.removeSuffix(":").removeSuffix("-").trim()
                    repository.insertAsset(ProductAsset(title = title, description = trimmedText, type = "PRODUK"))
                    repository.insertLog(AgentLog("Content Extractor 🤖", "Impor cadangan (produk tunggal) selesai.", "Judul: $title"))
                    onComplete(1)
                }
            } catch (e: Exception) {
                // Total fallback in case of no network or general error
                val firstLine = trimmedText.lines().firstOrNull()?.take(50) ?: "Aset Impor Tanpa Nama"
                val title = firstLine.removeSuffix(":").removeSuffix("-").trim()
                repository.insertAsset(ProductAsset(title = title, description = trimmedText, type = "PRODUK"))
                repository.insertLog(AgentLog("Content Extractor 🤖", "Terjadi error AI, teks dimasukkan sebagai produk tunggal.", "Error: ${e.message}"))
                onComplete(1)
            } finally {
                _isImportingProducts.value = false
                _importProgress.value = ""
            }
        }
    }

    // Add Product Asset Manually
    fun addManualProduct(title: String, description: String, type: String) {
        viewModelScope.launch {
            repository.insertAsset(ProductAsset(title = title, description = description, type = type))
            repository.insertLog(AgentLog("Content Extractor 🤖", "Menambahkan aset manual secara langsung.", "Tipe: $type, Judul: $title"))
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

    // ==========================================
    // EXTENDED CAPABILITIES (NEW ADDITIONS)
    // ==========================================

    private val prefs = getApplication<Application>().getSharedPreferences("otokreator_prefs", Context.MODE_PRIVATE)

    private val _weeklyCampaign = MutableStateFlow(
        prefs.getString("weekly_campaign", "") ?: ""
    )
    val weeklyCampaign: StateFlow<String> = _weeklyCampaign.asStateFlow()

    private val _isGeneratingWeeklyCampaign = MutableStateFlow(false)
    val isGeneratingWeeklyCampaign: StateFlow<Boolean> = _isGeneratingWeeklyCampaign.asStateFlow()

    fun generateWeeklyCampaign(onComplete: () -> Unit = {}) {
        if (_isGeneratingWeeklyCampaign.value) return
        viewModelScope.launch {
            _isGeneratingWeeklyCampaign.value = true
            repository.insertLog(AgentLog("Campaign Strategist 🎯", "Mulai merancang strategi kampanye mingguan berurutan.", "Menganalisis basis pengetahuan produk..."))
            
            val activeAssets = userProducts.value
            val assetsDesc = if (activeAssets.isEmpty()) {
                "Kopi Susu Literan Premium (OtoBrew)"
            } else {
                activeAssets.joinToString("\n") { "- [${it.type}] ${it.title}: ${it.description}" }
            }

            val prompt = """
                Berdasarkan daftar produk dan aset pemasaran berikut:
                $assetsDesc
                
                Rancanglah strategi tema kampanye mingguan berurutan selama 4 minggu ke depan. Setiap minggu harus memiliki satu tema besar yang unik, fokus pesan utama, jenis penawaran yang ideal (diskon, bundling, edukasi), dan 3 ide konten taktis (untuk TikTok, Instagram, Threads).
                
                Tuliskan strategi dalam bahasa Indonesia yang sangat profesional, terstruktur, kreatif, dan praktis bagi pebisnis. Gunakan format yang bersih dengan penomoran Minggu 1 s/d Minggu 4.
            """.trimIndent()

            try {
                val response = com.example.api.GeminiClient.generateContent(
                    prompt = prompt,
                    systemInstruction = "Anda adalah Chief Marketing Officer dan Ahli Strategi Kampanye Media Sosial Senior."
                )
                prefs.edit().putString("weekly_campaign", response).apply()
                _weeklyCampaign.value = response
                repository.insertLog(AgentLog("Campaign Strategist 🎯", "Sukses merancang tema kampanye 4 minggu!", "Rencana kampanye telah diperbarui."))
            } catch (e: Exception) {
                repository.insertLog(AgentLog("Campaign Strategist 🎯", "Gagal merancang kampanye mingguan.", e.message ?: "Unknown Error"))
            } finally {
                _isGeneratingWeeklyCampaign.value = false
                onComplete()
            }
        }
    }

    // Chat Consultation States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            sender = "AI",
            text = "Halo! Saya adalah Konsultan Ahli Pemasaran Anda di OtoKreator AI. 🎓\n\nSaya siap menjadi teman diskusi, memberi masukan strategi, me-review copywriting, atau memikirkan ide promo gila-gilaan agar bisnis Anda melejit. Apa yang ingin Anda tanyakan atau diskusikan hari ini?"
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isChatLoading.value) return
        
        viewModelScope.launch {
            _chatMessages.update { it + ChatMessage("USER", trimmed) }
            _isChatLoading.value = true
            
            val systemInstruction = """
                Anda adalah 'OtoKreator AI Expert Partner' - Pakar Marketing, Copywriter Senior, dan Ahli Strategi Konten Media Sosial legendaris di Indonesia.
                Tugas Anda adalah menjadi konsultan ahli, teman diskusi yang hidup, hangat, antusias, dan sangat berpengetahuan tinggi.
                Gaya komunikasi Anda: ramah, cerdas, solutif, menggunakan bahasa Indonesia yang santai tapi profesional (sesekali gunakan istilah marketing modern yang umum seperti hook, conversion, CTA secara natural).
                Berikan jawaban taktis yang bisa langsung dipraktekkan (berupa langkah konkret, rekomendasi format video, struktur copywriting AIDA, atau formula headline yang memicu klik).
                Selalu dukung dan apresiasi ide pengguna serta bantu mereka menyempurnakannya secara aktif.
            """.trimIndent()
            
            try {
                val history = _chatMessages.value.takeLast(6).joinToString("\n") { 
                    if (it.sender == "USER") "User: ${it.text}" else "AI: ${it.text}"
                }
                
                val response = com.example.api.GeminiClient.generateContent(
                    prompt = "$history\nUser: $trimmed\nAI:",
                    systemInstruction = systemInstruction
                )
                
                _chatMessages.update { it + ChatMessage("AI", response) }
            } catch (e: Exception) {
                _chatMessages.update { it + ChatMessage("AI", "Duh, sepertinya ada sedikit kendala koneksi dengan pusat pikiran saya. Bisakah Anda mengulangi pertanyaannya? 🙏 (Error: ${e.message})") }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // Post Scheduling Alarms
    fun schedulePostAlarm(context: Context, post: SocialPost, customHour: Int? = null, customMinute: Int? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ContentReminderReceiver::class.java).apply {
            putExtra("platform", post.platform)
            putExtra("slot", post.slotName)
            putExtra("caption", post.caption)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            post.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val targetHour = customHour ?: post.scheduleHour
        val targetMinute = customMinute ?: 0

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        
        val displayHour = targetHour.toString().padStart(2, '0')
        val displayMinute = targetMinute.toString().padStart(2, '0')

        prefs.edit().apply {
            putBoolean("alarm_active_${post.id}", true)
            putInt("alarm_time_hour_${post.id}", targetHour)
            putInt("alarm_time_minute_${post.id}", targetMinute)
        }.apply()
        
        Toast.makeText(context, "Alarm Posting ${post.platform} aktif untuk jam $displayHour:$displayMinute! 🔔", Toast.LENGTH_LONG).show()
        
        viewModelScope.launch {
            repository.insertLog(AgentLog("System", "Mengaktifkan Alarm Posting 🔔", "${post.platform} - Slot: ${post.slotName} pada jam $displayHour:$displayMinute"))
        }
    }

    fun cancelPostAlarm(context: Context, post: SocialPost) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ContentReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            post.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        prefs.edit().putBoolean("alarm_active_${post.id}", false).apply()
        Toast.makeText(context, "Alarm Posting ${post.platform} dinonaktifkan.", Toast.LENGTH_SHORT).show()
        
        viewModelScope.launch {
            repository.insertLog(AgentLog("System", "Menonaktifkan Alarm Posting 🔕", "${post.platform} - Slot: ${post.slotName}"))
        }
    }

    fun isAlarmActive(context: Context, postId: Int): Boolean {
        val p = context.getSharedPreferences("otokreator_prefs", Context.MODE_PRIVATE)
        return p.getBoolean("alarm_active_$postId", false)
    }
}

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

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
