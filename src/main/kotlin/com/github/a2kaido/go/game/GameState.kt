package com.github.a2kaido.go.game

import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.MoveAction
import com.github.a2kaido.go.model.Player

data class GameState(
    val board: Board,
    val nextPlayer: Player,
    val previousState: GameState?,
    val lastMove: Move?,
) {
    private val previousStates: List<Map<Player, Long>> = if (previousState == null) {
        listOf()
    } else {
        previousState.previousStates + (mapOf(previousState.nextPlayer to previousState.board.zobristHash()))
    }

    fun applyMove(move: Move): GameState {
        val nextBoard: Board
        if (move.action is MoveAction.Play) {
            nextBoard = board.copy(grid = board.grid.toMutableMap())
            nextBoard.placeStone(nextPlayer, move.action.point)
        } else {
            nextBoard = board
        }

        return GameState(
            board = nextBoard,
            nextPlayer = nextPlayer.other(),
            previousState = this,
            lastMove = move,
        )
    }

    fun isOver(): Boolean {
        if (lastMove == null) return false
        if (lastMove.action is MoveAction.Resign) return true
        val secondLastMove = previousState?.lastMove ?: return false
        return (lastMove.action is MoveAction.Pass) && secondLastMove.action is MoveAction.Pass
    }

    private fun isMoveSelfCapture(player: Player, move: Move): Boolean {
        if (move.action !is MoveAction.Play) return false
        val nextBoard = board.deepCopy()
        nextBoard.placeStone(player, move.action.point)
        val newString = nextBoard.getGoString(move.action.point)
        return newString?.numLiberties() == 0
    }

    private fun doesMoveViolateKo(player: Player, move: Move): Boolean {
        if (move.action !is MoveAction.Play) return false

        val nextBoard = board.deepCopy()
        nextBoard.placeStone(player, move.action.point)

        val nextSituation = mapOf(player.other() to nextBoard.zobristHash())
        return nextSituation in previousStates
    }

    fun isValidMove(move: Move): Boolean {
        if (isOver()) return false

        return when (move.action) {
            MoveAction.Resign,
            MoveAction.Pass -> {
                true
            }
            is MoveAction.Play -> {
                board.get(move.action.point) == null &&
                        isMoveSelfCapture(nextPlayer, move).not() &&
                        doesMoveViolateKo(nextPlayer, move).not()
            }
        }
    }

    companion object {
        fun newGame(boardSize: Int = 13): GameState {
            return GameState(
                board = Board(boardSize, boardSize, mutableMapOf()),
                nextPlayer = Player.Black,
                previousState = null,
                lastMove = null,
            )
        }
    }
}
