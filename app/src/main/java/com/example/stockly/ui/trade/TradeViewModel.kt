package com.example.stockly.ui.trade

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockly.data.repository.WatchlistRepository
import com.example.stockly.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TradeViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _submissionState = MutableLiveData<Resource<Unit>?>()
    val submissionState: LiveData<Resource<Unit>?> = _submissionState

    fun resetState() {
        _submissionState.value = null
    }

    fun submitTrade(symbol: String, shares: String, orderType: String, limitPrice: String) {
        val sharesInt = shares.toIntOrNull()
        if (sharesInt == null || sharesInt <= 0) {
            _submissionState.value = Resource.Error("shares")
            return
        }
        if (orderType.isBlank()) {
            _submissionState.value = Resource.Error("orderType")
            return
        }
        if ((orderType == "Limit" || orderType == "Stop")) {
            val price = limitPrice.toDoubleOrNull()
            if (price == null || price <= 0.0) {
                _submissionState.value = Resource.Error("price")
                return
            }
        }
        _submissionState.value = Resource.Loading()
        viewModelScope.launch {
            delay(1000)
            watchlistRepository.getWatchlistItem(symbol)?.let { entity ->
                watchlistRepository.removeFromWatchlist(entity)
            }
            _submissionState.value = Resource.Success(Unit)
        }
    }
}
