package com.example.stockly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockly.data.local.entity.UserStockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStockDao {

    @Query("SELECT * FROM user_stocks ORDER BY id DESC")
    fun getAllUserStocks(): Flow<List<UserStockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStock(entity: UserStockEntity)

    @Delete
    suspend fun deleteUserStock(entity: UserStockEntity)
}
