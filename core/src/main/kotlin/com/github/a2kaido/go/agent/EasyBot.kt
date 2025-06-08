package com.github.a2kaido.go.agent

import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.Point
import kotlin.random.Random

class EasyBot(private val config: AIConfig = AIConfig.forDifficulty(AIDifficulty.EASY)) : Agent {
    
    override fun selectMove(gameState: GameState): Move {
        // For easy level, add basic pattern recognition:
        // 1. Capture opponent stones (high priority)
        // 2. Save own stones from capture (high priority)
        // 3. Extend existing groups (medium priority)
        // 4. Make corner moves (medium priority)
        // 5. Make edge moves (low priority)
        // 6. Avoid filling own eyes
        
        val captureMoves = mutableListOf<Point>()
        val saveMoves = mutableListOf<Point>()
        val extendMoves = mutableListOf<Point>()
        val cornerMoves = mutableListOf<Point>()
        val edgeMoves = mutableListOf<Point>()
        val safeMoves = mutableListOf<Point>()
        
        (1..gameState.board.numRows).forEach { row ->
            (1..gameState.board.numCols).forEach { col ->
                val candidate = Point(row, col)
                val move = Move.play(candidate)
                
                if (!gameState.isValidMove(move)) return@forEach
                if (isPointAnEye(gameState.board, candidate, gameState.nextPlayer)) return@forEach
                
                when {
                    capturesOpponentStones(gameState, candidate) -> captureMoves.add(candidate)
                    savesOwnStones(gameState, candidate) -> saveMoves.add(candidate)
                    extendsOwnGroup(gameState, candidate) -> extendMoves.add(candidate)
                    isCornerMove(gameState.board, candidate) -> cornerMoves.add(candidate)
                    isEdgeMove(gameState.board, candidate) -> edgeMoves.add(candidate)
                    else -> safeMoves.add(candidate)
                }
            }
        }
        
        // Select move with priority order
        val moveOptions = when {
            captureMoves.isNotEmpty() -> captureMoves
            saveMoves.isNotEmpty() -> saveMoves
            extendMoves.isNotEmpty() -> extendMoves
            cornerMoves.isNotEmpty() -> cornerMoves
            edgeMoves.isNotEmpty() -> edgeMoves
            safeMoves.isNotEmpty() -> safeMoves
            else -> return Move.pass()
        }
        
        // Add randomization
        return if (config.randomizationLevel > 0 && Random.nextFloat() < config.randomizationLevel) {
            val allValidMoves = captureMoves + saveMoves + extendMoves + cornerMoves + edgeMoves + safeMoves
            if (allValidMoves.isNotEmpty()) {
                Move.play(allValidMoves.random())
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
                if (point in neighborString.liberties) {
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun extendsOwnGroup(gameState: GameState, point: Point): Boolean {
        val player = gameState.nextPlayer
        
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == player) {
                // This move would connect to our existing group
                return true
            }
        }
        
        return false
    }
    
    private fun isCornerMove(board: com.github.a2kaido.go.model.Board, point: Point): Boolean {
        val corners = listOf(
            Point(1, 1),
            Point(1, board.numCols),
            Point(board.numRows, 1),
            Point(board.numRows, board.numCols)
        )
        return point in corners
    }
    
    private fun isEdgeMove(board: com.github.a2kaido.go.model.Board, point: Point): Boolean {
        return point.row == 1 || point.row == board.numRows || 
               point.col == 1 || point.col == board.numCols
    }
}