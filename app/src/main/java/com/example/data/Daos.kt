package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductAssetDao {
    @Query("SELECT * FROM product_assets ORDER BY createdAt DESC")
    fun getAllAssets(): Flow<List<ProductAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: ProductAsset)

    @Query("DELETE FROM product_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Int)
}

@Dao
interface TrendTopicDao {
    @Query("SELECT * FROM trend_topics ORDER BY timestamp DESC")
    fun getAllTrends(): Flow<List<TrendTopic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrend(trend: TrendTopic)

    @Query("DELETE FROM trend_topics")
    suspend fun clearAllTrends()
}

@Dao
interface SocialPostDao {
    @Query("SELECT * FROM social_posts ORDER BY scheduleHour ASC, platform ASC")
    fun getAllPosts(): Flow<List<SocialPost>>

    @Query("SELECT * FROM social_posts WHERE platform = :platform ORDER BY scheduleHour ASC")
    fun getPostsByPlatform(platform: String): Flow<List<SocialPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SocialPost)

    @Query("UPDATE social_posts SET status = :status WHERE id = :id")
    suspend fun updatePostStatus(id: Int, status: String)

    @Query("DELETE FROM social_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)

    @Query("DELETE FROM social_posts")
    suspend fun clearAllPosts()
}

@Dao
interface AgentLogDao {
    @Query("SELECT * FROM agent_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<AgentLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AgentLog)

    @Query("DELETE FROM agent_logs")
    suspend fun clearLogs()
}
