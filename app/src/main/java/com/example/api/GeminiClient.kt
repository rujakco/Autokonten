package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext "Error: API Key Gemini belum dikonfigurasi. Silakan atur GEMINI_API_KEY di panel Secrets AI Studio atau file .env."
        }

        val requestUrl = "$BASE_URL?key=$apiKey"

        // Build request JSON manually or via Moshi
        val requestJson = buildRequestJson(prompt, systemInstruction)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${bodyString ?: "Unknown Error"}"
                    Log.e(TAG, errMsg)
                    return@withContext "Error: $errMsg"
                }

                if (bodyString.isNullOrEmpty()) {
                    return@withContext "Error: Response body is empty"
                }

                // Extract text from Gemini response JSON
                val extractedText = extractTextFromResponse(bodyString)
                if (extractedText != null) {
                    return@withContext extractedText
                } else {
                    Log.e(TAG, "Failed to parse Gemini response: $bodyString")
                    return@withContext "Error: Gagal memproses respons dari AI."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during call: ${e.message}", e)
            return@withContext "Error: ${e.message}"
        }
    }

    private fun buildRequestJson(prompt: String, systemInstruction: String?): String {
        // We can construct the simple JSON string manually to ensure total safety and speed
        val escapedPrompt = escapeJson(prompt)
        val systemInstructionPart = if (!systemInstruction.isNullOrEmpty()) {
            val escapedSys = escapeJson(systemInstruction)
            """
            "systemInstruction": {
                "parts": [
                    { "text": "$escapedSys" }
                ]
            },
            """.trimIndent()
        } else ""

        return """
        {
            $systemInstructionPart
            "contents": [
                {
                    "parts": [
                        { "text": "$escapedPrompt" }
                    ]
                }
            ],
            "generationConfig": {
                "temperature": 0.7
            }
        }
        """.trimIndent()
    }

    private fun extractTextFromResponse(json: String): String? {
        try {
            // Find "text" inside "content" -> "parts" using robust string token searching 
            // to bypass minor JSON structural fluctuations
            val candidatesStart = json.indexOf("\"candidates\"")
            if (candidatesStart == -1) return null
            
            val textStartToken = "\"text\":"
            var currentIndex = candidatesStart
            val firstTextIndex = json.indexOf(textStartToken, currentIndex)
            if (firstTextIndex == -1) return null

            val valueStartIndex = json.indexOf("\"", firstTextIndex + textStartToken.length) + 1
            var valueEndIndex = valueStartIndex
            var isEscaped = false
            
            while (valueEndIndex < json.length) {
                val char = json[valueEndIndex]
                if (isEscaped) {
                    isEscaped = false
                } else if (char == '\\') {
                    isEscaped = true
                } else if (char == '"') {
                    break
                }
                valueEndIndex++
            }
            
            val rawText = json.substring(valueStartIndex, valueEndIndex)
            return unescapeJson(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing json string search: ${e.message}", e)
        }
        return null
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
