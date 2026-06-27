package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_assets")
data class ProductAsset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trend_topics")
data class TrendTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val source: String, // e.g. "X / Twitter", "TikTok Trends", "Google News"
    val hotness: String, // e.g. "Tinggi 🔥", "Sedang ⚡", "Normal 📌"
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "social_posts")
data class SocialPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platform: String, // "TIKTOK", "INSTAGRAM", "THREADS"
    val caption: String,
    val hashtags: String,
    val imageUrl: String? = null, // Mock or generated visual asset URI
    val promptUsed: String,
    val scheduleHour: Int, // e.g. 8, 12, 17, 20, 22
    val scheduleMinute: Int = 0,
    val slotName: String, // e.g. "Pagi 🌅", "Siang ☀️", "Sore 🌇", "Malam 🌃", "Larut Malam 🌌"
    val status: String, // "DRAFT", "SCHEDULED", "PUBLISHED"
    val creativeIdea: String, // Connecting trend + product
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_logs")
data class AgentLog(
    val agentName: String, // e.g. "Trend Finder", "Creative Brainstormer", etc.
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
