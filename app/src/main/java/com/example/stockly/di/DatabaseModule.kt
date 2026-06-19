package com.example.stockly.di

import android.content.Context
import androidx.room.Room
import com.example.stockly.data.local.dao.PriceAlertDao
import com.example.stockly.data.local.dao.SnapshotDao
import com.example.stockly.data.local.dao.UserStockDao
import com.example.stockly.data.local.dao.WatchlistDao
import com.example.stockly.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "stockly_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWatchlistDao(database: AppDatabase): WatchlistDao =
        database.watchlistDao()

    @Provides
    fun provideUserStockDao(database: AppDatabase): UserStockDao =
        database.userStockDao()

    @Provides
    fun providePriceAlertDao(database: AppDatabase): PriceAlertDao =
        database.priceAlertDao()

    @Provides
    fun provideSnapshotDao(database: AppDatabase): SnapshotDao =
        database.snapshotDao()
}
