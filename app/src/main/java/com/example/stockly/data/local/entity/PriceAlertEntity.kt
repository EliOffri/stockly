package com.example.stockly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val condition: String,
    val targetPrice: Double,
    val note: String = "",
    val logoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
