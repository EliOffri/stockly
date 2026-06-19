package com.example.stockly.ui.snapshot

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.stockly.data.local.entity.SnapshotEntity
import com.example.stockly.data.local.entity.WatchlistEntity
import com.example.stockly.data.repository.SnapshotRepository
import com.example.stockly.data.repository.StocksRepository
import com.example.stockly.data.repository.WatchlistRepository
import com.example.stockly.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketSnapshotViewModel @Inject constructor(
    private val repository: SnapshotRepository,
    private val watchlistRepository: WatchlistRepository,
    private val stocksRepository: StocksRepository
) : ViewModel() {

    val snapshots: LiveData<List<SnapshotEntity>> = repository.getAllSnapshots().asLiveData()

    private val _photoUri = MutableLiveData<Uri?>()
    val photoUri: LiveData<Uri?> = _photoUri

    private val _location = MutableLiveData<Pair<Double, Double>?>()
    val location: LiveData<Pair<Double, Double>?> = _location

    private val _submissionState = MutableLiveData<Resource<Unit>?>()
    val submissionState: LiveData<Resource<Unit>?> = _submissionState

    private val _watchlistWithPrices = MutableLiveData<List<Pair<WatchlistEntity, Double?>>?>()
    val watchlistWithPrices: LiveData<List<Pair<WatchlistEntity, Double?>>?> = _watchlistWithPrices

    private val _watchlistLoading = MutableLiveData(false)
    val watchlistLoading: LiveData<Boolean> = _watchlistLoading

    fun setPhotoUri(uri: Uri) {
        _photoUri.value = uri
    }

    fun setLocation(latitude: Double, longitude: Double) {
        _location.value = Pair(latitude, longitude)
    }

    fun clearFormState() {
        _photoUri.value = null
        _location.value = null
        _submissionState.value = null
        _watchlistWithPrices.value = null
    }

    fun resetSubmissionState() {
        _submissionState.value = null
    }

    fun loadWatchlistPrices() {
        _watchlistLoading.value = true
        viewModelScope.launch {
            try {
                val watchlistItems = watchlistRepository.getAllWatchlist().first()
                val results = coroutineScope {
                    watchlistItems.map { entity ->
                        async {
                            val result = stocksRepository.getQuote(entity.symbol)
                            val price = if (result is Resource.Success && result.data!!.currentPrice > 0.0) {
                                result.data.currentPrice
                            } else null
                            Pair(entity, price)
                        }
                    }.awaitAll()
                }
                _watchlistWithPrices.value = results
            } catch (e: Exception) {
                _watchlistWithPrices.value = emptyList()
            } finally {
                _watchlistLoading.value = false
            }
        }
    }

    fun submitSnapshot() {
        val uri = _photoUri.value
        if (uri == null) {
            _submissionState.value = Resource.Error("photo")
            return
        }
        val loc = _location.value
        if (loc == null) {
            _submissionState.value = Resource.Error("location")
            return
        }
        _submissionState.value = Resource.Loading()
        viewModelScope.launch {
            repository.insertSnapshot(
                SnapshotEntity(
                    description = buildWatchlistSummary(),
                    photoUri = uri.toString(),
                    latitude = loc.first,
                    longitude = loc.second
                )
            )
            _submissionState.value = Resource.Success(Unit)
        }
    }

    fun deleteSnapshot(snapshot: SnapshotEntity) {
        viewModelScope.launch { repository.deleteSnapshot(snapshot) }
    }

    private fun buildWatchlistSummary(): String {
        val items = _watchlistWithPrices.value
        if (items.isNullOrEmpty()) return "Market snapshot"
        return items.joinToString("\n") { (entity, price) ->
            if (price != null) "${entity.symbol}: $${"%.2f".format(price)}"
            else "${entity.symbol}: N/A"
        }
    }
}
