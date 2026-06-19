package com.example.stockly.data.repository

import com.example.stockly.data.local.dao.WatchlistDao
import com.example.stockly.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao
) {

    fun getAllWatchlist(): Flow<List<WatchlistEntity>> =
        watchlistDao.getAllWatchlist()

    fun isInWatchlist(symbol: String): Flow<Boolean> =
        watchlistDao.isInWatchlist(symbol)

    suspend fun addToWatchlist(entity: WatchlistEntity) =
        watchlistDao.insertWatchlistItem(entity)

    suspend fun updateWatchlistItem(entity: WatchlistEntity) =
        watchlistDao.updateWatchlistItem(entity)

    suspend fun removeFromWatchlist(entity: WatchlistEntity) =
        watchlistDao.deleteWatchlistItem(entity)

    suspend fun getWatchlistItem(symbol: String): WatchlistEntity? =
        watchlistDao.getWatchlistItem(symbol)
}
