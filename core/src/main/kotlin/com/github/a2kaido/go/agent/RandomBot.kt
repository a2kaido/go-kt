package com.github.a2kaido.go.agent

import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.Point
import com.github.a2kaido.go.model.Player

class RandomBot : Agent {
    override fun selectMove(
        gameState: GameState,
    ): Move {
        // Enhanced random bot that:
        // 1. Prioritizes capturing opponent stones
        // 2. Avoids being captured when possible
        // 3. Doesn't fill own eyes
        // 4. Avoids obviously bad moves in own territory
        
        val captureMoves = mutableListOf<Point>()
        val safeMoves = mutableListOf<Point>()
        val neutralMoves = mutableListOf<Point>()

        (1..(gameState.board.numRows)).forEach { row ->
            (1..(gameState.board.numCols)).forEach { col ->
                val candidate = Point(row, col)
                val move = Move.play(candidate)
                
                if (!gameState.isValidMove(move)) return@forEach
                if (isPointAnEye(gameState.board, candidate, gameState.nextPlayer)) return@forEach
                
                when {
                    capturesOpponentStones(gameState, candidate) -> captureMoves.add(candidate)
                    !isInOwnTerritory(gameState, candidate) -> safeMoves.add(candidate)
                    else -> neutralMoves.add(candidate)
                }
            }
        }

        // Prefer captures, then safe moves, then neutral moves
        val moveOptions = when {
            captureMoves.isNotEmpty() -> captureMoves
            safeMoves.isNotEmpty() -> safeMoves
            neutralMoves.isNotEmpty() -> neutralMoves
            else -> return Move.pass()
        }

        return Move.play(moveOptions.random())
    }
    
    private fun capturesOpponentStones(gameState: GameState, point: Point): Boolean {
        val opponent = gameState.nextPlayer.other()
        
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == opponent && neighborString.numLiberties() == 1) {
                if (point in neighborString.liberties) {
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun isInOwnTerritory(gameState: GameState, point: Point): Boolean {
        val player = gameState.nextPlayer
        var ownStones = 0
        var opponentStones = 0
        
        // Check in a small radius around the point
        for (dr in -2..2) {
            for (dc in -2..2) {
                val checkPoint = Point(point.row + dr, point.col + dc)
                if (!gameState.board.isOnGrid(checkPoint)) continue
                
                when (gameState.board.get(checkPoint)) {
                    Player.Black -> {
                        if (player == Player.Black) ownStones++ else opponentStones++
                    }
                    Player.White -> {
                        if (player == Player.White) ownStones++ else opponentStones++
                    }
                    null -> { /* Empty point, do nothing */ }
                }
            }
        }
        
        // Consider it own territory if we have significantly more stones nearby
        return ownStones > opponentStones + 2
    }
}
