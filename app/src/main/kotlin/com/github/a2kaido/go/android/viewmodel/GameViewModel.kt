package com.github.a2kaido.go.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.a2kaido.go.agent.Agent
import com.github.a2kaido.go.agent.RandomBot
import com.github.a2kaido.go.android.ui.FinishReason
import com.github.a2kaido.go.android.ui.GameStatus
import com.github.a2kaido.go.android.ui.GameUiState
import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move
import com.github.a2kaido.go.model.MoveAction
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameState: GameState = GameState.newGame(9)
    private val gameHistory = mutableListOf(gameState)
    private var historyIndex = 0
    
    private val blackAgent: Agent? = null
    private val whiteAgent: Agent = RandomBot()
    
    private var aiJob: Job? = null

    init {
        updateUiState()
    }

    fun onCellClick(row: Int, col: Int) {
        if (_uiState.value.isThinking || _uiState.value.gameStatus !is GameStatus.ONGOING) {
            return
        }
        
        val currentPlayer = gameState.nextPlayer
        if ((currentPlayer == Player.Black && blackAgent != null) || 
            (currentPlayer == Player.White && whiteAgent != null)) {
            return
        }
        
        val point = Point(row, col)
        val move = Move.play(point)
        
        if (gameState.isValidMove(move)) {
            applyMove(move)
        }
    }

    fun onPassClick() {
        if (_uiState.value.isThinking || _uiState.value.gameStatus !is GameStatus.ONGOING) {
            return
        }
        
        val currentPlayer = gameState.nextPlayer
        if ((currentPlayer == Player.Black && blackAgent != null) || 
            (currentPlayer == Player.White && whiteAgent != null)) {
            return
        }
        
        applyMove(Move.pass())
    }

    fun onResignClick() {
        if (_uiState.value.isThinking || _uiState.value.gameStatus !is GameStatus.ONGOING) {
            return
        }
        
        val currentPlayer = gameState.nextPlayer
        if ((currentPlayer == Player.Black && blackAgent != null) || 
            (currentPlayer == Player.White && whiteAgent != null)) {
            return
        }
        
        applyMove(Move.resign())
    }

    fun onUndoClick() {
        if (_uiState.value.isThinking || !_uiState.value.canUndo) {
            return
        }
        
        if (historyIndex > 0) {
            historyIndex--
            gameState = gameHistory[historyIndex]
            updateUiState()
        }
    }

    fun onRedoClick() {
        if (_uiState.value.isThinking || !_uiState.value.canRedo) {
            return
        }
        
        if (historyIndex < gameHistory.size - 1) {
            historyIndex++
            gameState = gameHistory[historyIndex]
            updateUiState()
        }
    }

    fun onNewGameClick(boardSize: Int = 9) {
        aiJob?.cancel()
        gameState = GameState.newGame(boardSize)
        gameHistory.clear()
        gameHistory.add(gameState)
        historyIndex = 0
        updateUiState()
        
        checkAndMakeAiMove()
    }

    private fun applyMove(move: Move) {
        if (gameState.isValidMove(move)) {
            gameState = gameState.applyMove(move)
            
            if (historyIndex < gameHistory.size - 1) {
                gameHistory.subList(historyIndex + 1, gameHistory.size).clear()
            }
            gameHistory.add(gameState)
            historyIndex++
            
            updateUiState()
            
            if (!gameState.isOver()) {
                checkAndMakeAiMove()
            }
        }
    }

    private fun checkAndMakeAiMove() {
        val currentPlayer = gameState.nextPlayer
        val agent = when (currentPlayer) {
            Player.Black -> blackAgent
            Player.White -> whiteAgent
        }
        
        if (agent != null) {
            makeAiMove(agent)
        }
    }

    private fun makeAiMove(agent: Agent) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isThinking = true) }
            
            delay(500)
            
            val move = withContext(Dispatchers.Default) {
                agent.selectMove(gameState)
            }
            
            applyMove(move)
            
            _uiState.update { it.copy(isThinking = false) }
        }
    }

    private fun updateUiState() {
        val board = gameState.board
        val boardState = mutableMapOf<Point, Player>()
        
        for (row in 1..board.numRows) {
            for (col in 1..board.numCols) {
                val point = Point(row, col)
                board.get(point)?.let { player ->
                    boardState[point] = player
                }
            }
        }
        
        val gameStatus = when {
            gameState.isOver() -> {
                val winner = determineWinner()
                val reason = when (gameState.lastMove?.action) {
                    is MoveAction.Resign -> FinishReason.RESIGNATION
                    is MoveAction.Pass -> if (gameState.previousState?.lastMove?.action is MoveAction.Pass) {
                        FinishReason.TWO_PASSES
                    } else {
                        FinishReason.NORMAL
                    }
                    else -> FinishReason.NORMAL
                }
                GameStatus.FINISHED(winner, reason)
            }
            else -> GameStatus.ONGOING
        }
        
        val lastMove = when (val action = gameState.lastMove?.action) {
            is MoveAction.Play -> action.point
            else -> null
        }
        
        _uiState.value = GameUiState(
            boardSize = board.numRows,
            boardState = boardState,
            currentPlayer = gameState.nextPlayer,
            blackCaptures = calculateCaptures(Player.Black),
            whiteCaptures = calculateCaptures(Player.White),
            lastMove = lastMove,
            gameStatus = gameStatus,
            isThinking = _uiState.value.isThinking,
            canUndo = historyIndex > 0,
            canRedo = historyIndex < gameHistory.size - 1
        )
    }

    private fun calculateCaptures(player: Player): Int {
        var captures = 0
        for (state in gameHistory.subList(0, historyIndex + 1)) {
            val previousBoard = state.previousState?.board
            val currentBoard = state.board
            
            if (previousBoard != null) {
                for (row in 1..currentBoard.numRows) {
                    for (col in 1..currentBoard.numCols) {
                        val point = Point(row, col)
                        val previousStone = previousBoard.get(point)
                        val currentStone = currentBoard.get(point)
                        
                        if (previousStone == player.other() && currentStone == null) {
                            captures++
                        }
                    }
                }
            }
        }
        return captures
    }

    private fun determineWinner(): Player? {
        // Simple winner determination based on who didn't resign
        if (gameState.lastMove?.action is MoveAction.Resign) {
            return gameState.nextPlayer // The player who didn't resign wins
        }
        
        // For now, we'll just count territory in a simple way
        // In a real game, this would be more complex with proper scoring
        val blackStones = gameState.board.grid.values.count { it.color == Player.Black }
        val whiteStones = gameState.board.grid.values.count { it.color == Player.White }
        
        return when {
            blackStones > whiteStones -> Player.Black
            whiteStones > blackStones -> Player.White
            else -> null // Draw
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiJob?.cancel()
    }
}