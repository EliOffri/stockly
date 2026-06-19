package com.example.stockly.data.repository

import com.example.stockly.data.remote.api.StockApiService
import com.example.stockly.data.remote.model.CompanyProfile
import com.example.stockly.data.remote.model.NewsItem
import com.example.stockly.data.remote.model.Quote
import com.example.stockly.data.remote.model.SearchResult
import com.example.stockly.data.remote.model.Stock
import com.example.stockly.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StocksRepository @Inject constructor(
    private val apiService: StockApiService
) {

    companion object {
        private const val API_KEY = "d848odhr01qutij8b4mgd848odhr01qutij8b4n0"
        val DEFAULT_SYMBOLS = listOf(
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
            "META", "NVDA", "NFLX", "AMD", "INTC"
        )
    }

    suspend fun getDefaultStocks(): Resource<List<Stock>> {
        return try {
            val stocks = coroutineScope {
                DEFAULT_SYMBOLS.map { symbol ->
                    async {
                        try {
                            val profile = apiService.getCompanyProfile(symbol, API_KEY)
                            if (profile.name.isNotBlank()) {
                                Stock(
                                    symbol = symbol,
                                    name = profile.name,
                                    logoUrl = profile.logo,
                                    exchange = profile.exchange,
                                    industry = profile.finnhubIndustry
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.map { it.await() }.filterNotNull()
            }
            if (stocks.isEmpty()) {
                Resource.Error("Failed to load stocks. Check your connection.")
            } else {
                Resource.Success(stocks)
            }
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun getCompanyProfile(symbol: String): Resource<CompanyProfile> {
        return try {
            val profile = apiService.getCompanyProfile(symbol, API_KEY)
            if (profile.name.isNotBlank()) {
                Resource.Success(profile)
            } else {
                Resource.Error("Stock not found")
            }
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun searchStocks(query: String): Resource<List<SearchResult>> {
        return try {
            val response = apiService.searchSymbols(query, API_KEY)
            Resource.Success(response.result.filter { it.type == "Common Stock" }.take(20))
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun getStockDetail(symbol: String): Resource<Pair<CompanyProfile, Quote>> {
        return try {
            coroutineScope {
                val profileDeferred = async { apiService.getCompanyProfile(symbol, API_KEY) }
                val quoteDeferred = async { apiService.getQuote(symbol, API_KEY) }
                Resource.Success(Pair(profileDeferred.await(), quoteDeferred.await()))
            }
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun getQuote(symbol: String): Resource<Quote> {
        return try {
            val quote = apiService.getQuote(symbol, API_KEY)
            Resource.Success(quote)
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun getStockNews(symbol: String): Resource<List<NewsItem>> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val toDate = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val fromDate = sdf.format(calendar.time)
            val news = apiService.getCompanyNews(symbol, fromDate, toDate, API_KEY)
            Resource.Success(news.filter { it.image.isNotBlank() }.take(10))
        } catch (e: IOException) {
            Resource.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
}
