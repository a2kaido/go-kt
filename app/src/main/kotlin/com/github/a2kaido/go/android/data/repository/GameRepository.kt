package com.github.a2kaido.go.android.data.repository

import com.github.a2kaido.go.android.data.dao.MoveRecordDao
import com.github.a2kaido.go.android.data.dao.SavedGameDao
import com.github.a2kaido.go.android.data.entity.MoveRecord
import com.github.a2kaido.go.android.data.entity.SavedGame
import com.github.a2kaido.go.android.data.entity.SavedGameWithMoves
import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.GoString
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.MoveAction
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point
import kotlinx.coroutines.flow.Flow
import java.util.Date

class GameRepository(
    private val savedGameDao: SavedGameDao,
    private val moveRecordDao: MoveRecordDao
) {
    fun getAllGames(): Flow<List<SavedGame>> = savedGameDao.getAllGames()

    fun getInProgressGames(): Flow<List<SavedGame>> = savedGameDao.getInProgressGames()

    fun getCompletedGames(): Flow<List<SavedGame>> = savedGameDao.getCompletedGames()

    suspend fun getGameWithMoves(gameId: Long): SavedGameWithMoves? {
        return savedGameDao.getGameWithMoves(gameId)
    }

    suspend fun saveNewGame(
        gameState: GameState,
        blackPlayerName: String,
        whitePlayerName: String
    ): Long {
        val savedGame = SavedGame(
            createdAt = Date(),
            updatedAt = Date(),
            boardSize = gameState.board.numCols,
            blackPlayerName = blackPlayerName,
            whitePlayerName = whitePlayerName,
            currentPlayerColor = gameState.nextPlayer.name,
            gameStatus = "IN_PROGRESS",
            moveCount = 0,
            thumbnail = generateBoardThumbnail(gameState.board)
        )
        return savedGameDao.insertGame(savedGame)
    }

    suspend fun saveMove(
        gameId: Long,
        move: Move,
        player: Player,
        moveNumber: Int,
        capturedPoints: Set<Point> = emptySet()
    ) {
        val moveRecord = when (val action = move.action) {
            is MoveAction.Play -> MoveRecord(
                gameId = gameId,
                moveNumber = moveNumber,
                playerColor = player.name,
                moveType = "PLAY",
                row = action.point.row,
                col = action.point.col,
                capturedStones = if (capturedPoints.isNotEmpty()) {
                    capturedPoints.joinToString(",") { "${it.row},${it.col}" }
                } else null,
                timestamp = System.currentTimeMillis()
            )
            is MoveAction.Pass -> MoveRecord(
                gameId = gameId,
                moveNumber = moveNumber,
                playerColor = player.name,
                moveType = "PASS",
                timestamp = System.currentTimeMillis()
            )
            is MoveAction.Resign -> MoveRecord(
                gameId = gameId,
                moveNumber = moveNumber,
                playerColor = player.name,
                moveType = "RESIGN",
                timestamp = System.currentTimeMillis()
            )
        }

        moveRecordDao.insertMove(moveRecord)

        // Update game metadata
        val game = savedGameDao.getGameById(gameId)
        game?.let {
            savedGameDao.updateGame(
                it.copy(
                    updatedAt = Date(),
                    moveCount = moveNumber,
                    currentPlayerColor = player.other().name,
                    gameStatus = if (move.action is MoveAction.Resign) "COMPLETED" else "IN_PROGRESS",
                    winner = if (move.action is MoveAction.Resign) player.other().name else null
                )
            )
        }
    }

    suspend fun updateGameState(
        gameId: Long,
        gameState: GameState,
        gameDuration: Long
    ) {
        val game = savedGameDao.getGameById(gameId)
        game?.let {
            savedGameDao.updateGame(
                it.copy(
                    updatedAt = Date(),
                    currentPlayerColor = gameState.nextPlayer.name,
                    gameDuration = gameDuration,
                    thumbnail = generateBoardThumbnail(gameState.board)
                )
            )
        }
    }

    suspend fun completeGame(
        gameId: Long,
        winner: Player?,
        blackScore: Float,
        whiteScore: Float
    ) {
        val game = savedGameDao.getGameById(gameId)
        game?.let {
            savedGameDao.updateGame(
                it.copy(
                    updatedAt = Date(),
                    gameStatus = "COMPLETED",
                    winner = winner?.name,
                    blackScore = blackScore,
                    whiteScore = whiteScore
                )
            )
        }
    }

    suspend fun deleteGame(gameId: Long) {
        savedGameDao.deleteGameById(gameId)
    }

    suspend fun loadGameState(gameId: Long): GameState? {
        val gameWithMoves = getGameWithMoves(gameId) ?: return null
        
        var gameState = GameState.newGame(gameWithMoves.savedGame.boardSize)
        
        for (moveRecord in gameWithMoves.moves.sortedBy { it.moveNumber }) {
            val move = when (moveRecord.moveType) {
                "PLAY" -> {
                    if (moveRecord.row != null && moveRecord.col != null) {
                        Move.play(Point(moveRecord.row, moveRecord.col))
                    } else {
                        continue
                    }
                }
                "PASS" -> Move.pass()
                "RESIGN" -> Move.resign()
                else -> continue
            }
            
            gameState = gameState.applyMove(move)
        }
        
        return gameState
    }

    private fun generateBoardThumbnail(board: Board): String {
        // Simple placeholder for now - will implement proper thumbnail later
        return "${board.numRows}x${board.numCols}"
    }
}