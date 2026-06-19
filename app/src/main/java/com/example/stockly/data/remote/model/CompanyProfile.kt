package com.example.stockly.data.remote.model

data class CompanyProfile(
    val country: String = "",
    val currency: String = "",
    val exchange: String = "",
    val ipo: String = "",
    val logo: String = "",
    val marketCapitalization: Double = 0.0,
    val name: String = "",
    val shareOutstanding: Double = 0.0,
    val ticker: String = "",
    val weburl: String = "",
    val finnhubIndustry: String = ""
)
