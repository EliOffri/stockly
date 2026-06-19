package com.example.stockly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockly.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {

    @Query("SELECT * FROM snapshots ORDER BY createdAt DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    @Delete
    suspend fun deleteSnapshot(snapshot: SnapshotEntity)
}
