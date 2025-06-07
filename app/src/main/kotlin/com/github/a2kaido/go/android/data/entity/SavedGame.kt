package com.github.a2kaido.go.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Date,
    val updatedAt: Date,
    val boardSize: Int,
    val blackPlayerName: String,
    val whitePlayerName: String,
    val currentPlayerColor: String, // "BLACK" or "WHITE"
    val gameStatus: String, // "IN_PROGRESS", "COMPLETED", "RESIGNED"
    val winner: String? = null, // "BLACK", "WHITE", or null
    val blackScore: Float? = null,
    val whiteScore: Float? = null,
    val moveCount: Int = 0,
    val gameDuration: Long = 0, // milliseconds
    val thumbnail: String? = null // Base64 encoded board state for preview
)