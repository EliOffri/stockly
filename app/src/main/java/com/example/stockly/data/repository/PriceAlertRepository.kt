package com.example.stockly.data.repository

import com.example.stockly.data.local.dao.PriceAlertDao
import com.example.stockly.data.local.entity.PriceAlertEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceAlertRepository @Inject constructor(private val dao: PriceAlertDao) {

    fun getAllAlerts(): Flow<List<PriceAlertEntity>> = dao.getAllAlerts()

    suspend fun insertAlert(alert: PriceAlertEntity) = dao.insertAlert(alert)

    suspend fun deleteAlert(alert: PriceAlertEntity) = dao.deleteAlert(alert)

    suspend fun updateAlert(alert: PriceAlertEntity) = dao.updateAlert(alert)
}
