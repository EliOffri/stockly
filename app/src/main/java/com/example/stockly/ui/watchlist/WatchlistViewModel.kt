package com.example.stockly.ui.watchlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.stockly.data.local.entity.WatchlistEntity
import com.example.stockly.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    val watchlist: LiveData<List<WatchlistEntity>> =
        watchlistRepository.getAllWatchlist().asLiveData()

    fun removeFromWatchlist(entity: WatchlistEntity) {
        viewModelScope.launch {
            watchlistRepository.removeFromWatchlist(entity)
        }
    }

    fun updateNotes(entity: WatchlistEntity, newNotes: String) {
        viewModelScope.launch {
            watchlistRepository.updateWatchlistItem(entity.copy(notes = newNotes))
        }
    }
}
