package com.example.stockly.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stockly.data.local.dao.PriceAlertDao
import com.example.stockly.data.local.dao.SnapshotDao
import com.example.stockly.data.local.dao.UserStockDao
import com.example.stockly.data.local.dao.WatchlistDao
import com.example.stockly.data.local.entity.PriceAlertEntity
import com.example.stockly.data.local.entity.SnapshotEntity
import com.example.stockly.data.local.entity.UserStockEntity
import com.example.stockly.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class, UserStockEntity::class, PriceAlertEntity::class, SnapshotEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun userStockDao(): UserStockDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun snapshotDao(): SnapshotDao
}
