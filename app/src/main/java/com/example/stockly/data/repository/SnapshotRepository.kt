package com.example.stockly.data.repository

import com.example.stockly.data.local.dao.SnapshotDao
import com.example.stockly.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepository @Inject constructor(private val dao: SnapshotDao) {

    fun getAllSnapshots(): Flow<List<SnapshotEntity>> = dao.getAllSnapshots()

    suspend fun insertSnapshot(snapshot: SnapshotEntity) = dao.insertSnapshot(snapshot)

    suspend fun deleteSnapshot(snapshot: SnapshotEntity) = dao.deleteSnapshot(snapshot)
}
