package com.github.a2kaido.go.agent

import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.Point
import kotlin.random.Random

class BeginnerBot(private val config: AIConfig = AIConfig.forDifficulty(AIDifficulty.BEGINNER)) : Agent {
    
    override fun selectMove(gameState: GameState): Move {
        // For beginners, prioritize:
        // 1. Capturing opponent stones
        // 2. Avoiding being captured
        // 3. Random moves that don't fill own eyes
        
        val candidates = mutableListOf<Point>()
        val priorityMoves = mutableListOf<Point>()
        val avoidCaptureMoves = mutableListOf<Point>()
        
        (1..gameState.board.numRows).forEach { row ->
            (1..gameState.board.numCols).forEach { col ->
                val candidate = Point(row, col)
                val move = Move.play(candidate)
                
                if (!gameState.isValidMove(move)) return@forEach
                if (isPointAnEye(gameState.board, candidate, gameState.nextPlayer)) return@forEach
                
                candidates.add(candidate)
                
                // Check if this move captures opponent stones
                if (capturesOpponentStones(gameState, candidate)) {
                    priorityMoves.add(candidate)
                }
                
                // Check if this move saves our stones from capture
                if (savesOwnStones(gameState, candidate)) {
                    avoidCaptureMoves.add(candidate)
                }
            }
        }
        
        // Select move with priority:
        // 1. Moves that capture opponent stones
        // 2. Moves that save our stones from capture
        // 3. Random valid moves
        
        val moveOptions = when {
            priorityMoves.isNotEmpty() -> priorityMoves
            avoidCaptureMoves.isNotEmpty() -> avoidCaptureMoves
            candidates.isNotEmpty() -> candidates
            else -> return Move.pass()
        }
        
        // Add some randomization for beginner level
        return if (config.randomizationLevel > 0 && Random.nextFloat() < config.randomizationLevel) {
            if (candidates.isNotEmpty()) {
                Move.play(candidates.random())
            } else {
                Move.pass()
            }
        } else {
            Move.play(moveOptions.random())
        }
    }
    
    private fun capturesOpponentStones(gameState: GameState, point: Point): Boolean {
        val opponent = gameState.nextPlayer.other()
        
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == opponent && neighborString.numLiberties() == 1) {
                // This opponent group has only one liberty - placing here would capture it
                if (point in neighborString.liberties) {
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun savesOwnStones(gameState: GameState, point: Point): Boolean {
        val player = gameState.nextPlayer
        
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == player && neighborString.numLiberties() == 1) {
                // Our group has only one liberty - placing here would give it more liberties
                if (point in neighborString.liberties) {
                    return true
                }
            }
        }
        
        return false
    }
}