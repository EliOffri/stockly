package com.example.stockly.data.remote.model

import com.google.gson.annotations.SerializedName

data class Quote(
    @SerializedName("c") val currentPrice: Double = 0.0,
    @SerializedName("d") val change: Double = 0.0,
    @SerializedName("dp") val changePercent: Double = 0.0,
    @SerializedName("h") val highPrice: Double = 0.0,
    @SerializedName("l") val lowPrice: Double = 0.0,
    @SerializedName("o") val openPrice: Double = 0.0,
    @SerializedName("pc") val previousClose: Double = 0.0
)
