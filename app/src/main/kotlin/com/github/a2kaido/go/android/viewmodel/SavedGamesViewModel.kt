package com.github.a2kaido.go.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.a2kaido.go.android.data.GoDatabase
import com.github.a2kaido.go.android.data.entity.SavedGame
import com.github.a2kaido.go.android.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedGamesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GoDatabase.getDatabase(application)
    private val repository = GameRepository(database.savedGameDao(), database.moveRecordDao())

    private val _savedGames = MutableStateFlow<List<SavedGame>>(emptyList())
    val savedGames: StateFlow<List<SavedGame>> = _savedGames.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSavedGames()
    }

    private fun loadSavedGames() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllGames().collect { games ->
                _savedGames.value = games
                _isLoading.value = false
            }
        }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
        }
    }

    fun refresh() {
        loadSavedGames()
    }
}