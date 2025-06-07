package com.github.a2kaido.go.game

import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.MoveAction
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point
import com.github.a2kaido.go.model.GoString

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
        val point = move.action.point
        
        // Check if the move would result in self-capture
        // We need to check before the Board automatically removes self-capturing stones
        val liberties = mutableListOf<Point>()
        val adjacentSameColor = mutableListOf<GoString>()
        val adjacentOppositeColor = mutableListOf<GoString>()
        
        point.neighbors().forEach { neighbor ->
            if (board.isOnGrid(neighbor).not()) return@forEach
            
            val neighborString = board.getGoString(neighbor)
            if (neighborString == null) {
                liberties.add(neighbor)
            } else if (neighborString.color == player) {
                if ((neighborString in adjacentSameColor).not()) {
                    adjacentSameColor.add(neighborString)
                }
            } else {
                if ((neighborString in adjacentOppositeColor).not()) {
                    adjacentOppositeColor.add(neighborString)
                }
            }
        }
        
        // Calculate liberties after merging with same-color groups
        var totalLiberties: MutableSet<Point> = liberties.toMutableSet()
        adjacentSameColor.forEach { sameColorString ->
            totalLiberties.addAll(sameColorString.liberties)
        }
        
        // Remove the point we're placing on (it won't be a liberty anymore)
        totalLiberties.remove(point)
        
        // Check if any opponent groups would be captured
        val wouldCaptureOpponent = adjacentOppositeColor.any { opponentString ->
            opponentString.liberties.size == 1 && point in opponentString.liberties
        }
        
        // If we would capture opponent stones, the move is not self-capture
        if (wouldCaptureOpponent) return false
        
        // If we have no liberties after placement, it's self-capture
        return totalLiberties.isEmpty()
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
                val isMoveSelfCapture = isMoveSelfCapture(nextPlayer, move)
                val doesMoveViolateKo = doesMoveViolateKo(nextPlayer, move)
                board.get(move.action.point) == null &&
                        isMoveSelfCapture.not() &&
                        doesMoveViolateKo.not()
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
