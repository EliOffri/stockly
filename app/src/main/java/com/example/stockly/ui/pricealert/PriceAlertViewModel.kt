package com.example.stockly.ui.pricealert

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.stockly.data.local.entity.PriceAlertEntity
import com.example.stockly.data.repository.PriceAlertRepository
import com.example.stockly.data.repository.StocksRepository
import com.example.stockly.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriceAlertViewModel @Inject constructor(
    private val repository: PriceAlertRepository,
    private val stocksRepository: StocksRepository
) : ViewModel() {

    val alerts: LiveData<List<PriceAlertEntity>> = repository.getAllAlerts().asLiveData()

    private val _submissionState = MutableLiveData<Resource<Unit>?>()
    val submissionState: LiveData<Resource<Unit>?> = _submissionState

    fun resetState() {
        _submissionState.value = null
    }

    fun submitAlert(editingId: Int?, symbol: String, condition: String, targetPrice: String, note: String) {
        if (symbol.isBlank()) {
            _submissionState.value = Resource.Error("symbol")
            return
        }
        if (symbol.uppercase() !in StocksRepository.DEFAULT_SYMBOLS) {
            _submissionState.value = Resource.Error("invalid_symbol")
            return
        }
        if (condition.isBlank()) {
            _submissionState.value = Resource.Error("condition")
            return
        }
        val price = targetPrice.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            _submissionState.value = Resource.Error("price")
            return
        }
        _submissionState.value = Resource.Loading()
        viewModelScope.launch {
            val logoResult = stocksRepository.getCompanyProfile(symbol.uppercase())
            val logoUrl = if (logoResult is Resource.Success) logoResult.data?.logo ?: "" else ""
            if (editingId != null) {
                repository.updateAlert(
                    PriceAlertEntity(
                        id = editingId,
                        symbol = symbol.uppercase(),
                        condition = condition,
                        targetPrice = price,
                        note = note.trim(),
                        logoUrl = logoUrl
                    )
                )
            } else {
                repository.insertAlert(
                    PriceAlertEntity(
                        symbol = symbol.uppercase(),
                        condition = condition,
                        targetPrice = price,
                        note = note.trim(),
                        logoUrl = logoUrl
                    )
                )
            }
            _submissionState.value = Resource.Success(Unit)
        }
    }

    fun deleteAlert(alert: PriceAlertEntity) {
        viewModelScope.launch { repository.deleteAlert(alert) }
    }
}
