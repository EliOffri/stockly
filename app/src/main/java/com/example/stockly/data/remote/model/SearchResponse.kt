package com.example.stockly.data.remote.model

data class SearchResponse(
    val count: Int = 0,
    val result: List<SearchResult> = emptyList()
)

data class SearchResult(
    val description: String = "",
    val displaySymbol: String = "",
    val symbol: String = "",
    val type: String = ""
)
