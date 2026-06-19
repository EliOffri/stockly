package com.example.stockly.ui.market

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.stockly.data.local.entity.UserStockEntity
import com.example.stockly.data.local.entity.WatchlistEntity
import com.example.stockly.data.remote.model.Stock
import com.example.stockly.data.repository.StocksRepository
import com.example.stockly.data.repository.UserStocksRepository
import com.example.stockly.data.repository.WatchlistRepository
import com.example.stockly.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val stocksRepository: StocksRepository,
    private val userStocksRepository: UserStocksRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _apiStocks = MutableLiveData<Resource<List<Stock>>>(Resource.Loading())

    val combinedList: LiveData<Resource<List<Stock>>> =
        userStocksRepository.getAllUserStocks().asLiveData().switchMap { userStocks ->
            _apiStocks.map { apiState ->
                when (apiState) {
                    is Resource.Success -> {
                        val localItems = userStocks.map { it.toStock() }
                        Resource.Success(localItems + (apiState.data ?: emptyList()))
                    }
                    is Resource.Error -> apiState
                    is Resource.Loading -> apiState
                }
            }
        }

    private val _featuredSymbol = MutableLiveData<String>()
    val isFeaturedInWatchlist: LiveData<Boolean> = _featuredSymbol.switchMap { symbol ->
        watchlistRepository.isInWatchlist(symbol).asLiveData()
    }

    private val _addStockState = MutableLiveData<Resource<Unit>>()
    val addStockState: LiveData<Resource<Unit>> = _addStockState

    fun setFeaturedStock(symbol: String) {
        _featuredSymbol.value = symbol
    }

    fun toggleWatchlist(stock: Stock) {
        viewModelScope.launch {
            val existing = watchlistRepository.getWatchlistItem(stock.symbol)
            if (existing != null) {
                watchlistRepository.removeFromWatchlist(existing)
            } else {
                watchlistRepository.addToWatchlist(
                    WatchlistEntity(symbol = stock.symbol, name = stock.name, logoUrl = stock.logoUrl)
                )
            }
        }
    }

    init {
        loadStocks()
    }

    fun loadStocks() {
        _apiStocks.value = Resource.Loading()
        viewModelScope.launch {
            _apiStocks.value = stocksRepository.getDefaultStocks()
        }
    }

    fun addUserStock(symbol: String, name: String, logoUrl: String) {
        val upperSymbol = symbol.uppercase().trim()
        if (upperSymbol.isBlank()) return
        viewModelScope.launch {
            _addStockState.value = Resource.Loading()
            val result = stocksRepository.getCompanyProfile(upperSymbol)
            when (result) {
                is Resource.Success -> {
                    val profile = result.data!!
                    userStocksRepository.addUserStock(
                        UserStockEntity(
                            symbol = upperSymbol,
                            name = name.ifBlank { profile.name.ifBlank { upperSymbol } },
                            logoUrl = logoUrl.ifBlank { profile.logo }
                        )
                    )
                    _addStockState.value = Resource.Success(Unit)
                }
                is Resource.Error -> {
                    userStocksRepository.addUserStock(
                        UserStockEntity(
                            symbol = upperSymbol,
                            name = name.ifBlank { upperSymbol },
                            logoUrl = logoUrl
                        )
                    )
                    _addStockState.value = Resource.Success(Unit)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteUserStock(stock: Stock) {
        val id = stock.localId ?: return
        viewModelScope.launch {
            userStocksRepository.deleteUserStock(
                UserStockEntity(id = id, symbol = stock.symbol, name = stock.name, logoUrl = stock.logoUrl)
            )
        }
    }

    private fun UserStockEntity.toStock(): Stock =
        Stock(
            symbol = symbol,
            name = name,
            logoUrl = logoUrl,
            isLocallyAdded = true,
            localId = id
        )
}
