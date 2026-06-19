package com.example.stockly.data.remote.api

import com.example.stockly.data.remote.model.CompanyProfile
import com.example.stockly.data.remote.model.NewsItem
import com.example.stockly.data.remote.model.Quote
import com.example.stockly.data.remote.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {

    @GET("search")
    suspend fun searchSymbols(
        @Query("q") query: String,
        @Query("token") token: String
    ): SearchResponse

    @GET("stock/profile2")
    suspend fun getCompanyProfile(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): CompanyProfile

    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): Quote

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") token: String
    ): List<NewsItem>
}
