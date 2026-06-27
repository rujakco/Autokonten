package com.example.api

import android.util.Log
import com.example.data.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

object AgentOrchestrator {
    private const val TAG = "AgentOrchestrator"

    suspend fun runAutomationChain(
        repository: OtoRepository,
        userProducts: List<ProductAsset>,
        onProgressUpdate: (String) -> Unit
    ): Boolean {
        try {
            // 1. Preparation
            onProgressUpdate("Mempersiapkan tim agen...")
            repository.insertLog(AgentLog("System", "Memulai rantai agen otomatis super konten kreator.", "Mempersiapkan parameter produk dan trend."))

            // Fallback product if none input
            val products = if (userProducts.isEmpty()) {
                val sampleProduct = ProductAsset(
                    title = "Kopi Susu Literan Premium (OtoBrew)",
                    description = "Kopi susu gula aren premium dalam kemasan botol 1 liter. Dibuat dari biji kopi arabika pilihan, susu segar creamy, dan gula aren organik cair. Tahan 5 hari di kulkas, cocok untuk menemani kerja seharian."
                )
                repository.insertAsset(sampleProduct)
                repository.insertLog(AgentLog("System", "Menambahkan produk sampel karena produk kosong.", "Nama: ${sampleProduct.title}"))
                listOf(sampleProduct)
            } else {
                userProducts
            }

            val productInfo = products.joinToString("\n\n") { "- ${it.title}: ${it.description}" }

            // ==========================================
            // STEP 2: RUN TREND FINDER AGENT
            // ==========================================
            onProgressUpdate("Trend Finder: Mencari topik hangat di sosmed & berita...")
            repository.insertLog(AgentLog("Trend Finder", "Mencari topik hangat terbaru di media sosial & berita.", "Memindai tren lokal dan global yang cocok untuk audiens Indonesia."))

            val trendPrompt = """
                Anda adalah Trend Finder Agent. Tugas Anda adalah memindai internet untuk mencari 3 topik hangat, tren musik viral, atau berita terpopuler yang sedang naik daun di Indonesia saat ini (Bulan Juni 2026).
                Beri kami 3 tren yang paling relate dengan media sosial (TikTok, Instagram, dan Threads).
                
                Format respon harus tepat seperti ini agar dapat diparsing aplikasi:
                [TREND]
                Judul: <Judul tren singkat>
                Sumber: <TikTok / Instagram / Twitter / Berita>
                Hotness: <Tinggi 🔥 / Sedang ⚡ / Normal 📌>
                Ringkasan: <Ringkasan mengapa tren ini naik daun dan apa intinya>
                [END_TREND]
                
                Berikan tepat 3 tren dalam format tersebut. Jangan ada teks pembuka atau penutup lain.
            """.trimIndent()

            val trendResult = GeminiClient.generateContent(
                prompt = trendPrompt,
                systemInstruction = "Anda adalah agen pencari tren media sosial profesional."
            )

            // Parse and save trends
            val trends = parseAndSaveTrends(repository, trendResult)
            if (trends.isEmpty()) {
                // Fallback trends
                val fbTrends = listOf(
                    TrendTopic(title = "Tren Produktivitas Minimalis & Slow Living", source = "TikTok & Threads", hotness = "Tinggi 🔥", summary = "Banyak konten kreator membagikan rutinitas kerja santai namun produktif dengan visual estetik."),
                    TrendTopic(title = "Krisis Kafein & Kebutuhan 'Focus Drink' saat WFH", source = "Instagram Reels", hotness = "Sedang ⚡", summary = "Tren membahas minuman kopi literan yang hemat dan berkualitas untuk stok selama WFH."),
                    TrendTopic(title = "Tantangan 30 Hari Tanpa Jajan di Luar", source = "TikTok", hotness = "Tinggi 🔥", summary = "Masyarakat berhemat dengan beralih ke makanan/minuman kemasan botol besar di rumah.")
                )
                for (t in fbTrends) repository.insertTrend(t)
                repository.insertLog(AgentLog("Trend Finder", "Menggunakan fallback tren lokal Indonesia.", "Gagal memparsing atau API offline, beralih ke tren bawaan."))
            } else {
                repository.insertLog(AgentLog("Trend Finder", "Berhasil menemukan ${trends.size} tren baru.", trends.joinToString("\n") { "- ${it.title} (${it.source})" }))
            }

            val currentTrends = repository.allTrends.first().take(3)
            val trendInfo = currentTrends.joinToString("\n\n") { "- ${it.title} (${it.source}): ${it.summary}" }

            // ==========================================
            // STEP 3: RUN CREATIVE BRAINSTORMER AGENT
            // ==========================================
            onProgressUpdate("Creative Brainstormer: Merancang ideasi konsep kreatif...")
            repository.insertLog(AgentLog("Creative Brainstormer", "Menggabungkan data produk dengan tren viral.", "Mencari korelasi unik antara produk dan ketertarikan audiens."))

            val creativePrompt = """
                Anda adalah Creative Brainstormer Agent. Tugas Anda adalah memikirkan cara kreatif dan inovatif untuk menggabungkan produk pengguna dengan tren-tren terhangat yang sedang viral.
                
                PRODUK MILIK PENGGUNA:
                $productInfo
                
                TREN VIRAL SAAT INI:
                $trendInfo
                
                Rancang 3 Konsep Kolaborasi Kreatif Tanpa Batas yang akan menjadi fondasi konten media sosial hari ini.
                
                Format respon harus singkat, cerdas, dan langsung memberikan:
                1. Judul Konsep
                2. Tren yang Digunakan
                3. Sudut Pandang Kreatif (Bagaimana produk diletakkan di tengah tren secara alami dan tidak hard-selling)
            """.trimIndent()

            val creativeResult = GeminiClient.generateContent(
                prompt = creativePrompt,
                systemInstruction = "Anda adalah pengatur strategi kreatif dan copywriter legendaris."
            )

            repository.insertLog(AgentLog("Creative Brainstormer", "Berhasil menyusun 3 strategi kreatif harian.", creativeResult))

            // ==========================================
            // STEP 4: RUN CONTENT PLANNER & PROMPT ENGINEER AGENT
            // ==========================================
            onProgressUpdate("Content Planner & Prompt Engineer: Menyusun 15 Konten Harian...")
            repository.insertLog(AgentLog("Content Planner & Prompt Engineer", "Menjadwalkan 5 slot konten untuk TikTok, Instagram, & Threads.", "Menyesuaikan waktu upload dengan psikologi audiens di waktu tersebut (Pagi, Siang, Sore, Malam, Larut Malam)."))

            val plannerPrompt = """
                Anda adalah Content Planner & Prompt Engineer Agent. Tugas utama Anda adalah membuat 5 konten harian untuk 3 media sosial utama: TIKTOK, INSTAGRAM, dan THREADS (Total 15 konten).
                Semua konten harus dibuat secara cerdas agar relate dengan waktu uploadnya!
                
                DATA REFERENSI:
                Produk: $productInfo
                Konsep Kreatif Hari Ini: $creativeResult
                
                SLOT WAKTU UPLOAD & PSIKOLOGI AUDIENS:
                1. Pagi (08:00): Motivasi memulai hari, kesegaran, konten produktivitas, vibes kopi pagi.
                2. Siang (12:00): Makan siang, hiburan ringan, humor relasi kantor/kuliah, tips cepat 30 detik.
                3. Sore (17:00): Pulang kerja, lelah butuh santai, konten POV estetik, review jujur, unboxing.
                4. Malam (20:00): Prime time! Edukasi mendalam, storytelling, komedi situasi, promo diskon kilat.
                5. Larut Malam (22:00): Refleksi malam, deep talk, aesthetic mood, diskon malam hari untuk kaum insomnia.
                
                Tuliskan konten dalam format tag [POST] ... [END_POST] berikut. ANDA HARUS MEMBERIKAN TEPAT 15 POSTINGAN (5 slot x 3 platform).
                Patuhi format ini secara ketat untuk setiap post agar aplikasi kami bisa mem-parsingnya dengan sukses!
                
                [POST]
                Platform: <TIKTOK atau INSTAGRAM atau THREADS>
                Slot: <Pagi / Siang / Sore / Malam / Larut Malam>
                Jam: <8 atau 12 या 17 atau 20 atau 22>
                Konsep: <Penjelasan ide video/visual singkat>
                Caption: <Caption menarik gaya lokal Indonesia, gunakan bahasa santai/casual, sesuaikan platform (TikTok: hook kuat, Threads: percakapan & personal, IG: estetik & detail)>
                Hashtags: <Hashtag dipisahkan spasi>
                VisualPrompt: <Prompt detail dalam bahasa inggris untuk membuat gambar AI atau deskripsi visual video>
                [END_POST]
            """.trimIndent()

            val plannerResult = GeminiClient.generateContent(
                prompt = plannerPrompt,
                systemInstruction = "Anda adalah pengatur konten media sosial dan insinyur prompt AI handal."
            )

            // Clear old posts and parse new ones
            repository.clearAllPosts()
            val postsCount = parseAndSavePosts(repository, plannerResult, creativeResult)

            if (postsCount == 0) {
                // If parsing failed or API limit reached, insert high-quality mock posts
                insertSamplePosts(repository)
                repository.insertLog(AgentLog("Content Planner & Prompt Engineer", "Gagal memparsing respons AI secara otomatis.", "Menggunakan templat konten berkualitas tinggi yang disiapkan secara dinamis."))
            } else {
                repository.insertLog(AgentLog("Content Planner & Prompt Engineer", "Sukses menyusun jadwal!", "Menghasilkan $postsCount konten siap tayang (TikTok, Instagram, Threads)."))
            }

            // ==========================================
            // STEP 5: RUN AUTO-PUBLISHER AGENT (PLANNER INITIAL LOG)
            // ==========================================
            onProgressUpdate("Selesai! Jadwal konten Anda telah aktif.")
            repository.insertLog(AgentLog("Auto-Publisher", "Sistem penjadwalan konten aktif.", "Semua konten (15 postingan) telah dialokasikan ke slot masing-masing. Tekan tombol 'Bagikan Sekarang' di tab Eksekusi untuk memposting langsung."))

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in automation chain: ${e.message}", e)
            repository.insertLog(AgentLog("System", "Kesalahan Sistem Utama", "Pesan error: ${e.message}"))
            return false
        }
    }

    private suspend fun parseAndSaveTrends(repository: OtoRepository, text: String): List<TrendTopic> {
        val trends = mutableListOf<TrendTopic>()
        try {
            val blocks = text.split("[TREND]")
            for (i in 1 until blocks.size) {
                val block = blocks[i].split("[END_TREND]")[0]
                
                var title = ""
                var source = ""
                var hotness = ""
                var summary = ""

                val lines = block.trim().lines()
                for (line in lines) {
                    when {
                        line.startsWith("Judul:") -> title = line.substringAfter("Judul:").trim()
                        line.startsWith("Sumber:") -> source = line.substringAfter("Sumber:").trim()
                        line.startsWith("Hotness:") -> hotness = line.substringAfter("Hotness:").trim()
                        line.startsWith("Ringkasan:") -> summary = line.substringAfter("Ringkasan:").trim()
                    }
                }

                if (title.isNotEmpty()) {
                    val trend = TrendTopic(
                        title = title,
                        source = if (source.isEmpty()) "Media Sosial" else source,
                        hotness = if (hotness.isEmpty()) "Tinggi 🔥" else hotness,
                        summary = if (summary.isEmpty()) "Sedang ramai diperbincangkan." else summary
                    )
                    repository.insertTrend(trend)
                    trends.add(trend)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing trends: ${e.message}")
        }
        return trends
    }

    private suspend fun parseAndSavePosts(repository: OtoRepository, text: String, creativeBrief: String): Int {
        var count = 0
        try {
            val blocks = text.split("[POST]")
            for (i in 1 until blocks.size) {
                val block = blocks[i].split("[END_POST]")[0]
                
                var platform = ""
                var slot = ""
                var jamStr = ""
                var konsep = ""
                var caption = ""
                var hashtags = ""
                var visualPrompt = ""

                val lines = block.trim().lines()
                var currentField = ""
                val fieldBuilder = StringBuilder()

                // Robust multi-line field reading
                for (line in lines) {
                    val trimmedLine = line.trim()
                    when {
                        trimmedLine.startsWith("Platform:") -> {
                            platform = trimmedLine.substringAfter("Platform:").trim().uppercase()
                        }
                        trimmedLine.startsWith("Slot:") -> {
                            slot = trimmedLine.substringAfter("Slot:").trim()
                        }
                        trimmedLine.startsWith("Jam:") -> {
                            jamStr = trimmedLine.substringAfter("Jam:").trim()
                        }
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
                
                // Final flush of builders
                if (currentField == "visual") {
                    visualPrompt = fieldBuilder.toString().trim()
                } else if (currentField == "hashtags") {
                    hashtags = fieldBuilder.toString().trim()
                } else if (currentField == "caption") {
                    caption = fieldBuilder.toString().trim()
                }

                // Sanitize platform
                if (!platform.contains("TIKTOK") && !platform.contains("INSTAGRAM") && !platform.contains("THREADS")) {
                    // Try to guess from text
                    platform = when {
                        block.contains("TIKTOK", ignoreCase = true) -> "TIKTOK"
                        block.contains("INSTAGRAM", ignoreCase = true) -> "INSTAGRAM"
                        else -> "THREADS"
                    }
                }
                // Clean exact matching TIKTOK, INSTAGRAM, THREADS
                if (platform.contains("TIKTOK")) platform = "TIKTOK"
                if (platform.contains("INSTAGRAM")) platform = "INSTAGRAM"
                if (platform.contains("THREADS")) platform = "THREADS"

                val hour = jamStr.filter { it.isDigit() }.toIntOrNull() ?: when (slot) {
                    "Pagi" -> 8
                    "Siang" -> 12
                    "Sore" -> 17
                    "Malam" -> 20
                    else -> 22
                }

                val slotWithIcon = when {
                    slot.contains("Pagi", ignoreCase = true) -> "Pagi 🌅"
                    slot.contains("Siang", ignoreCase = true) -> "Siang ☀️"
                    slot.contains("Sore", ignoreCase = true) -> "Sore 🌇"
                    slot.contains("Malam", ignoreCase = true) -> "Malam 🌃"
                    else -> "Larut Malam 🌌"
                }

                if (caption.isNotEmpty()) {
                    val post = SocialPost(
                        platform = platform,
                        caption = caption,
                        hashtags = hashtags,
                        promptUsed = visualPrompt.ifEmpty { "Dynamic studio layout depicting product details in aesthetic environment" },
                        scheduleHour = hour,
                        slotName = slotWithIcon,
                        status = "SCHEDULED",
                        creativeIdea = konsep.ifEmpty { "Gabungan kreatif produk dengan preferensi visual audiens." }
                    )
                    repository.insertPost(post)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing posts: ${e.message}")
        }
        return count
    }

    private suspend fun insertSamplePosts(repository: OtoRepository) {
        val samples = listOf(
            // TIKTOK
            SocialPost(
                platform = "TIKTOK",
                slotName = "Pagi 🌅",
                scheduleHour = 8,
                caption = "WFH tapi berasa di cafe premium? ☕✨ Stok kopi literan di kulkas adalah kunci! Sekali tuang, kerja jadi lancar jaya seharian tanpa ribet go-food berkali-kali. Lebih hemat, tetep nikmat!",
                hashtags = "#OtoBrew #KopiSusuLiteran #WFHLife #WorkspaceSetup #AnakKafein #SlowLivingIndonesia",
                promptUsed = "A warm cinematic shot of a modern clean workspace with a glass bottle of premium iced latte next to a sleek laptop, warm morning sunlight casting beautiful long shadows, 8k resolution, photorealistic",
                status = "SCHEDULED",
                creativeIdea = "Menghubungkan tren slow living di pagi hari dengan kepraktisan kopi susu literan siap tuang."
            ),
            SocialPost(
                platform = "TIKTOK",
                slotName = "Siang ☀️",
                scheduleHour = 12,
                caption = "Kondisi jam 12 siang pas kerjaan numpuk tapi mata merem melek... 🥱 Gak usah panik, tuang OtoBrew dingin langsung melek seketika! Gula aren organiknya kasih energi instan biar lanjut gass kerjaan.",
                hashtags = "#KopiSiang #MelekSeketika #OtoBrewKopi #WFHHumor #ProduktifSiang #KopiSusuEnak",
                promptUsed = "Top-down view of an aesthetic workstation with an overflowing glass of creamy iced coffee with bubbles, ice cubes melting, energetic bright studio lighting, flat lay, cozy creative style",
                status = "SCHEDULED",
                creativeIdea = "POV humor relasi produktivitas kantor di jam makan siang yang mengantuk."
            ),
            SocialPost(
                platform = "TIKTOK",
                slotName = "Sore 🌇",
                scheduleHour = 17,
                caption = "Akhirnya jam pulang kantor tiba! 🎉 Saatnya reward diri sendiri dengan segelas kopi susu premium dingin. Duduk santai di balkon dengerin musik favorit sambil nikmatin creamy-nya OtoBrew.",
                hashtags = "#SoreRelax #HealingDiRumah #KopiBalkon #RewardDiri #OtoBrewMoment #BalkonEstetik",
                promptUsed = "Cozy apartment balcony at sunset, golden hour light, hands holding a cold glass of milk coffee, soft warm lighting, cinematic depth of field",
                status = "SCHEDULED",
                creativeIdea = "POV relaksasi sore hari selepas kerja keras seharian."
            ),
            SocialPost(
                platform = "TIKTOK",
                slotName = "Malam 🌃",
                scheduleHour = 20,
                caption = "PROMO KILAT MALAM INI SAJA! 💥 Stok kopi literan OtoBrew buat seminggu ke depan diskon 20%! Jangan sampai kehabisan karena produksi fresh setiap pagi terbatas. Klik keranjang kuning sekarang!",
                hashtags = "#DiskonKilat #KopiLiteran #StokSeminggu #AnakPromo #OtoBrewKopiSusu #BelanjaHemat",
                promptUsed = "Dramatic studio shot of three dark glass bottles of coffee milk arranged in a line, backlighting highlighting the rich caramel color, modern minimal background with neon accents, ultra-premium look",
                status = "SCHEDULED",
                creativeIdea = "Penawaran promo malam hari bertepatan dengan prime-time scroll sosmed."
            ),
            SocialPost(
                platform = "TIKTOK",
                slotName = "Larut Malam 🌌",
                scheduleHour = 22,
                caption = "Buat yang suka kebangun tengah malam nyari yang seger-seger di kulkas... OtoBrew selalu siap sedia nemenin maraton film atau nugas malam Anda. Estetik, creamy, bikin malam makin tenang.",
                hashtags = "#KaumInsomnia #MaratonFilm #NugasMalam #KopiDingin #AnakMalam #OtoBrewVibes",
                promptUsed = "A dimly lit room with a blue light glow from a TV screen, a modern nightstand with a half-empty glass of creamy coffee, moody dark aesthetic, quiet atmosphere",
                status = "SCHEDULED",
                creativeIdea = "Relate dengan kebiasaan insomnia kaum muda di malam hari."
            ),

            // INSTAGRAM
            SocialPost(
                platform = "INSTAGRAM",
                slotName = "Pagi 🌅",
                scheduleHour = 8,
                caption = "Aesthetic mornings start with good coffee and clean setups. ☕🌿 Memulai hari Senin dengan penuh semangat ditemani segelas OtoBrew Premium yang super creamy dan praktis. Ready to conquer the week!",
                hashtags = "#AestheticMorning #WorkspaceInspiration #CoffeeFirst #MinimalistSetup #CreamyLatte #InstagramIndo",
                promptUsed = "A bright minimalist wooden table, a green monstera plant in a white pot, a clear glass with creamy iced coffee showing beautiful swirls of milk, soft morning light",
                status = "SCHEDULED",
                creativeIdea = "Visual estetik minimalis meja kerja pagi hari untuk audiens Instagram."
            ),
            SocialPost(
                platform = "INSTAGRAM",
                slotName = "Siang ☀️",
                scheduleHour = 12,
                caption = "Lunch break companion! 🥗☕ Gak perlu antre di coffee shop lagi panas-panas begini. Cukup ambil OtoBrew botolan dari kulkas kantor, tuang pakai es batu, and you are ready to recharge!",
                hashtags = "#LunchBreak #KopiBotolan #CoffeeStash #WFHLunch #PraktisDanHemat #KopiSusuGulaAren",
                promptUsed = "Close-up of a premium glass bottle of coffee with a well-designed craft label, fresh sweat droplets on the bottle, vibrant colorful background with summer vibes",
                status = "SCHEDULED",
                creativeIdea = "Menyoroti nilai kepraktisan (anti-antre dan anti-panas) di jam istirahat siang."
            ),
            SocialPost(
                platform = "INSTAGRAM",
                slotName = "Sore 🌇",
                scheduleHour = 17,
                caption = "Golden hour is better with cold coffee. 🌇 Cobain deh kombinasi OtoBrew dingin dengan biskuit favoritmu pas sunset. Kombinasi manis aren organik dan susu creamy-nya pas banget di lidah.",
                hashtags = "#GoldenHourVibes #SoreEstetik #SnackTime #SunsetCoffee #PecintaKopi #CoffeeWithView",
                promptUsed = "Hands holding a vintage coffee mug filled with espresso, sitting by a large window overlooking a city skyline at sunset, warm cinematic mood",
                status = "SCHEDULED",
                creativeIdea = "Menciptakan mood estetik sore hari yang sangat disukai audiens Instagram."
            ),
            SocialPost(
                platform = "INSTAGRAM",
                slotName = "Malam 🌃",
                scheduleHour = 20,
                caption = "Unboxing kebahagiaan mingguan! 📦✨ Stok OtoBrew Kopi Susu Gula Aren 1 Liter untuk nemenin WFH kamu seminggu penuh. Dibuat fresh setiap hari tanpa pengawet buatan.",
                hashtags = "#UnboxingVideo #CoffeeStash #StokKopi #KopiFresh #KopiSusuBotol #PecintaKopiSusu",
                promptUsed = "A stylish cardboard delivery box opened on a wooden floor, revealing chilled glass bottles of coffee nestled in straw packaging, warm cozy overhead lighting",
                status = "SCHEDULED",
                creativeIdea = "Review unboxing stok kopi mingguan yang menonjolkan aspek kesegaran dan higienitas."
            ),
            SocialPost(
                platform = "INSTAGRAM",
                slotName = "Larut Malam 🌌",
                scheduleHour = 22,
                caption = "Midnight thoughts: Ada ketenangan tersendiri saat menikmati sisa kopi dingin di keheningan malam sambil merencanakan esok hari. Tetap tenang, esok akan luar biasa. 🌙✨",
                hashtags = "#MidnightThoughts #DeepTalk #RefleksiMalam #AestheticLatte #TenangJiwa #OtoBrewAesthetic",
                promptUsed = "Dark moody setting, a single warm glowing desk lamp illuminating a notebook and a beautiful glass of iced milk coffee, soft focus background",
                status = "SCHEDULED",
                creativeIdea = "Konten deep-talk reflektif malam hari yang bernuansa tenang dan estetik."
            ),

            // THREADS
            SocialPost(
                platform = "THREADS",
                slotName = "Pagi 🌅",
                scheduleHour = 8,
                caption = "Tim kopi susu gula aren buatan sendiri vs tim beli go-food tiap pagi: kalian hemat berapa ratus ribu sebulan? 🤫 Sejak beralih ke kopi susu literan OtoBrew, pengeluaran kopi terpangkas setengah tapi tetep bisa minum kopi kualitas kafe tiap pagi. Diskusi di bawah 👇",
                hashtags = "#KopiHemat #FinansialHack #AnakMudaProduktif #StokKopi",
                promptUsed = "Text centric casual layout featuring comparison stats of daily spending vs bulk buying premium coffee",
                status = "SCHEDULED",
                creativeIdea = "Mengajak interaksi dan diskusi finansial ringan di pagi hari khas gaya Threads."
            ),
            SocialPost(
                platform = "THREADS",
                slotName = "Siang ☀️",
                scheduleHour = 12,
                caption = "Kerja dari rumah (WFH) tapi bos malah kirim kerjaan mendadak pas lagi makan siang... Cara paling ampuh buat nahan emosi: minum kopi susu dingin manis aren pelan-pelan. Setuju ga? Apa penawar stres kalian?",
                hashtags = "#WFHCulture #KerjaKantor #PenahanEmosi #KopiSusuPenyelamat",
                promptUsed = "Close up illustrative reaction of a relaxing cup of coffee contrasting chaotic workspace papers in soft background",
                status = "SCHEDULED",
                creativeIdea = "Relatable rants tentang dinamika WFH/kantor di jam makan siang."
            ),
            SocialPost(
                platform = "THREADS",
                slotName = "Sore 🌇",
                scheduleHour = 17,
                caption = "Sore-sore begini enaknya dengerin lagu indie, rebahan, sambil minum kopi susu dingin langsung dari botolnya. Definisi self-care paling murah tapi mewah. Gimana cara kalian melepas penat sore ini?",
                hashtags = "#SoreTenang #SelfCare #PecintaKopiSusu #RebahanEstetik",
                promptUsed = "Calming hand-drawn minimal illustration of a coffee mug sitting on a windowsill next to a stack of books",
                status = "SCHEDULED",
                creativeIdea = "Diskusi hangat seputar self-care dan cara melepas penat di sore hari."
            ),
            SocialPost(
                platform = "THREADS",
                slotName = "Malam 🌃",
                scheduleHour = 20,
                caption = "Sering dapet keluhan kopi botolan literan rasanya hambar kalau dikasih es batu? Tips dari barista OtoBrew: buat es batu dari sisa kopi susu kamu kemarin! Pas mencair, kopi kamu malah makin pekat dan mantap! Cobain deh dan thank me later! 💡☕",
                hashtags = "#LifeHack #BaristaTips #CoffeeTips #OtoBrewHack #PecintaKopi",
                promptUsed = "Educational infographic depicting steps to freeze coffee cubes for iced latte, clean vector look",
                status = "SCHEDULED",
                creativeIdea = "Membagikan life hack/tips barista berharga untuk mengundang interaksi di malam hari."
            ),
            SocialPost(
                platform = "THREADS",
                slotName = "Larut Malam 🌌",
                scheduleHour = 22,
                caption = "Kenapa ya ide-ide kreatif brilian itu justru selalu muncul jam 10 malam ke atas pas kita udah mau tidur? 🧠 Terpaksa deh bangun lagi, ambil OtoBrew dingin di kulkas, terus catat semua idenya biar gak lupa besok pagi. Siapa yang begini juga?",
                hashtags = "#KaumKreatif #IdeMalam #MidnightBrainstorm #InsomniaKreatif",
                promptUsed = "Minimalist starry night sky background with a glowing lightbulb outline next to a coffee cup silhouette",
                status = "SCHEDULED",
                creativeIdea = "Mengaitkan kecenderungan berpikir kreatif di malam hari dengan kebiasaan insomnia produktif."
            )
        )

        for (s in samples) {
            repository.insertPost(s)
        }
    }
}
