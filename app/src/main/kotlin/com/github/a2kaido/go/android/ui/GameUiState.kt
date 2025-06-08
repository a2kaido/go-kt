package com.github.a2kaido.go.android.ui

import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point

import androidx.compose.ui.geometry.Offset

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
    val canRedo: Boolean = false,
    val hoverPoint: Point? = null,
    val invalidMoveAttempt: Point? = null,
    val zoomScale: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val animatingStones: Set<Point> = emptySet(),
    val capturedStones: Set<Point> = emptySet(),
    val animationsEnabled: Boolean = true
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