package com.github.a2kaido.go.agent

import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.Point

class RandomBot : Agent {
    override fun selectMove(
        gameState: GameState,
    ): Move {
        // Choose a random valid move that preserves our own eyes.
        val candidates = mutableListOf<Point>()

        (1..(gameState.board.numRows)).forEach { row ->
            (1..(gameState.board.numCols)).forEach { col ->
                val candidate = Point(row, col)
                if (
                    gameState.isValidMove(Move.play(candidate)) &&
                    isPointAnEye(gameState.board, candidate, gameState.nextPlayer).not()
               ) {
                    candidates.add(candidate)
                }
            }
        }

        if (candidates.isEmpty()) return Move.pass()

        return Move.play(candidates.random())
    }
}
