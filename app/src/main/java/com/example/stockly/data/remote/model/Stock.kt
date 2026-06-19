package com.example.stockly.data.remote.model

data class Stock(
    val symbol: String,
    val name: String,
    val logoUrl: String,
    val currentPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val exchange: String = "",
    val industry: String = "",
    val isLocallyAdded: Boolean = false,
    val localId: Int? = null
)
