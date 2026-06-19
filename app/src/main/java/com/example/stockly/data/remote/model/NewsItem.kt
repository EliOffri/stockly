package com.example.stockly.data.remote.model

data class NewsItem(
    val datetime: Long = 0L,
    val headline: String = "",
    val id: Long = 0L,
    val image: String = "",
    val source: String = "",
    val summary: String = "",
    val url: String = ""
)
