package com.example.data

import kotlinx.coroutines.flow.Flow

class OtoRepository(private val db: AppDatabase) {
    private val productAssetDao = db.productAssetDao()
    private val trendTopicDao = db.trendTopicDao()
    private val socialPostDao = db.socialPostDao()
    private val agentLogDao = db.agentLogDao()

    // Product Assets
    val allAssets: Flow<List<ProductAsset>> = productAssetDao.getAllAssets()
    suspend fun insertAsset(asset: ProductAsset) = productAssetDao.insertAsset(asset)
    suspend fun deleteAssetById(id: Int) = productAssetDao.deleteAssetById(id)

    // Trend Topics
    val allTrends: Flow<List<TrendTopic>> = trendTopicDao.getAllTrends()
    suspend fun insertTrend(trend: TrendTopic) = trendTopicDao.insertTrend(trend)
    suspend fun clearAllTrends() = trendTopicDao.clearAllTrends()

    // Social Posts
    val allPosts: Flow<List<SocialPost>> = socialPostDao.getAllPosts()
    fun getPostsByPlatform(platform: String): Flow<List<SocialPost>> = socialPostDao.getPostsByPlatform(platform)
    suspend fun insertPost(post: SocialPost) = socialPostDao.insertPost(post)
    suspend fun updatePostStatus(id: Int, status: String) = socialPostDao.updatePostStatus(id, status)
    suspend fun deletePostById(id: Int) = socialPostDao.deletePostById(id)
    suspend fun clearAllPosts() = socialPostDao.clearAllPosts()

    // Agent Logs
    val allLogs: Flow<List<AgentLog>> = agentLogDao.getAllLogs()
    suspend fun insertLog(log: AgentLog) = agentLogDao.insertLog(log)
    suspend fun clearLogs() = agentLogDao.clearLogs()
}
