package com.github.a2kaido.go.android.ui

import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point

data class GameUiState(
    val boardSize: Int = 9,
    val boardState: Map<Point, Player> = emptyMap(),
    val currentPlayer: Player = Player.Black,
    val blackCaptures: Int = 0,
    val whiteCaptures: Int = 0,
    val lastMove: Point? = null,
    val gameStatus: GameStatus = GameStatus.ONGOING,
    val isThinking: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

sealed class GameStatus {
    data object ONGOING : GameStatus()
    data class FINISHED(val winner: Player?, val reason: FinishReason) : GameStatus()
}

enum class FinishReason {
    NORMAL,
    RESIGNATION,
    TWO_PASSES
}