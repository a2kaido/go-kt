package com.github.a2kaido.go.agent

import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.Point
import com.github.a2kaido.go.model.Player
import kotlin.random.Random

class MediumBot(private val config: AIConfig = AIConfig.forDifficulty(AIDifficulty.MEDIUM)) : Agent {
    
    override fun selectMove(gameState: GameState): Move {
        // For medium level, evaluate territory and strategic positions:
        // 1. Capture opponent stones (high priority)
        // 2. Save own stones from capture (high priority)
        // 3. Make moves that secure territory (medium priority)
        // 4. Block opponent territory formation (medium priority)
        // 5. Extend own groups strategically (medium priority)
        // 6. Make opening moves on key points (low priority)
        
        val moveScores = mutableMapOf<Point, Double>()
        
        (1..gameState.board.numRows).forEach { row ->
            (1..gameState.board.numCols).forEach { col ->
                val candidate = Point(row, col)
                val move = Move.play(candidate)
                
                if (!gameState.isValidMove(move)) return@forEach
                if (isPointAnEye(gameState.board, candidate, gameState.nextPlayer)) return@forEach
                
                val score = evaluateMove(gameState, candidate)
                if (score > 0) {
                    moveScores[candidate] = score
                }
            }
        }
        
        if (moveScores.isEmpty()) {
            return Move.pass()
        }
        
        // Add randomization by selecting from top moves
        val sortedMoves = moveScores.toList().sortedByDescending { it.second }
        val topMoves = if (config.randomizationLevel > 0) {
            val topCount = maxOf(1, (sortedMoves.size * (1 - config.randomizationLevel)).toInt())
            sortedMoves.take(topCount)
        } else {
            listOf(sortedMoves.first())
        }
        
        val selectedMove = topMoves.random()
        return Move.play(selectedMove.first)
    }
    
    private fun evaluateMove(gameState: GameState, point: Point): Double {
        var score = 0.0
        
        // Capture moves get highest priority
        if (capturesOpponentStones(gameState, point)) {
            score += 100.0
            val captureCount = countCapturedStones(gameState, point)
            score += captureCount * 10.0
        }
        
        // Saving own stones is also high priority
        if (savesOwnStones(gameState, point)) {
            score += 90.0
            val savedCount = countSavedStones(gameState, point)
            score += savedCount * 8.0
        }
        
        // Territory influence
        score += evaluateTerritoryInfluence(gameState, point)
        
        // Strategic position value
        score += evaluateStrategicValue(gameState, point)
        
        // Connection value
        score += evaluateConnectionValue(gameState, point)
        
        return score
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
    
    private fun countCapturedStones(gameState: GameState, point: Point): Int {
        val opponent = gameState.nextPlayer.other()
        var count = 0
        
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == opponent && neighborString.numLiberties() == 1) {
                if (point in neighborString.liberties) {
                    count += neighborString.stones.size
                }
            }
        }
        
        return count
    }
    
    private fun countSavedStones(gameState: GameState, point: Point): Int {
        val player = gameState.nextPlayer
        var count = 0
        
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == player && neighborString.numLiberties() == 1) {
                if (point in neighborString.liberties) {
                    count += neighborString.stones.size
                }
            }
        }
        
        return count
    }
    
    private fun evaluateTerritoryInfluence(gameState: GameState, point: Point): Double {
        // Simple territory evaluation based on distance to existing stones
        var influence = 0.0
        val player = gameState.nextPlayer
        
        // Check influence in a small radius around the point
        for (dr in -2..2) {
            for (dc in -2..2) {
                val checkPoint = Point(point.row + dr, point.col + dc)
                if (!gameState.board.isOnGrid(checkPoint)) continue
                
                val distance = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                val stone = gameState.board.get(checkPoint)
                
                when (stone) {
                    Player.Black -> {
                        if (player == Player.Black) {
                            influence += (3 - distance) * 2.0
                        } else {
                            influence -= (3 - distance) * 1.5
                        }
                    }
                    Player.White -> {
                        if (player == Player.White) {
                            influence += (3 - distance) * 2.0
                        } else {
                            influence -= (3 - distance) * 1.5
                        }
                    }
                    null -> influence += 0.5 // Empty points are slightly good
                }
            }
        }
        
        return influence
    }
    
    private fun evaluateStrategicValue(gameState: GameState, point: Point): Double {
        var value = 0.0
        
        // Corner moves are valuable early game
        if (isCorner(gameState.board, point)) {
            value += 5.0
        }
        
        // Edge moves have some value
        if (isEdge(gameState.board, point)) {
            value += 2.0
        }
        
        // Center points have moderate value
        val centerRow = gameState.board.numRows / 2
        val centerCol = gameState.board.numCols / 2
        val distanceFromCenter = maxOf(
            kotlin.math.abs(point.row - centerRow),
            kotlin.math.abs(point.col - centerCol)
        )
        value += (gameState.board.numRows - distanceFromCenter) * 0.1
        
        return value
    }
    
    private fun evaluateConnectionValue(gameState: GameState, point: Point): Double {
        var value = 0.0
        val player = gameState.nextPlayer
        
        var ownGroupsNearby = 0
        point.neighbors().forEach { neighbor ->
            if (!gameState.board.isOnGrid(neighbor)) return@forEach
            
            val neighborString = gameState.board.getGoString(neighbor)
            if (neighborString?.color == player) {
                ownGroupsNearby++
                value += 3.0 // Connection value
                
                // Bonus for connecting weak groups
                if (neighborString.numLiberties() <= 2) {
                    value += 5.0
                }
            }
        }
        
        // Bonus for connecting multiple groups
        if (ownGroupsNearby >= 2) {
            value += 10.0
        }
        
        return value
    }
    
    private fun isCorner(board: com.github.a2kaido.go.model.Board, point: Point): Boolean {
        val corners = listOf(
            Point(1, 1),
            Point(1, board.numCols),
            Point(board.numRows, 1),
            Point(board.numRows, board.numCols)
        )
        return point in corners
    }
    
    private fun isEdge(board: com.github.a2kaido.go.model.Board, point: Point): Boolean {
        return point.row == 1 || point.row == board.numRows || 
               point.col == 1 || point.col == board.numCols
    }
}