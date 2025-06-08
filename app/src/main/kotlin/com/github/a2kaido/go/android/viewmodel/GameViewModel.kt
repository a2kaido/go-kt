package com.github.a2kaido.go.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.a2kaido.go.agent.Agent
import com.github.a2kaido.go.agent.RandomBot
import com.github.a2kaido.go.agent.AIDifficulty
import com.github.a2kaido.go.agent.AIFactory
import com.github.a2kaido.go.android.ui.screens.PlayerType
import com.github.a2kaido.go.android.data.GoDatabase
import com.github.a2kaido.go.android.data.repository.GameRepository
import com.github.a2kaido.go.android.feedback.FeedbackManager
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

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val database = GoDatabase.getDatabase(application)
    private val repository = GameRepository(database.savedGameDao(), database.moveRecordDao())
    private val feedbackManager = FeedbackManager(application, viewModelScope)

    private var gameState: GameState = GameState.newGame(9)
    private val gameHistory = mutableListOf(gameState)
    private var historyIndex = 0
    
    private var blackAgent: Agent? = null
    private var whiteAgent: Agent? = RandomBot()
    
    private var aiJob: Job? = null
    private var currentGameId: Long? = null
    private var gameStartTime: Long = System.currentTimeMillis()
    private var moveCount = 0

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
        } else {
            // Show invalid move feedback
            feedbackManager.onInvalidMove()
            _uiState.update { it.copy(invalidMoveAttempt = point) }
            
            // Clear the invalid move indicator after a short delay
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(invalidMoveAttempt = null) }
            }
        }
    }

    fun onCellHover(row: Int, col: Int) {
        if (_uiState.value.isThinking || _uiState.value.gameStatus !is GameStatus.ONGOING) {
            _uiState.update { it.copy(hoverPoint = null) }
            return
        }
        
        val currentPlayer = gameState.nextPlayer
        if ((currentPlayer == Player.Black && blackAgent != null) || 
            (currentPlayer == Player.White && whiteAgent != null)) {
            _uiState.update { it.copy(hoverPoint = null) }
            return
        }
        
        val point = Point(row, col)
        val move = Move.play(point)
        
        if (gameState.isValidMove(move)) {
            _uiState.update { it.copy(hoverPoint = point) }
        } else {
            _uiState.update { it.copy(hoverPoint = null) }
        }
    }

    fun onHoverExit() {
        _uiState.update { it.copy(hoverPoint = null) }
    }

    fun onZoomPanChange(scale: Float, offset: androidx.compose.ui.geometry.Offset) {
        _uiState.update { it.copy(zoomScale = scale, panOffset = offset) }
    }

    fun resetZoomPan() {
        _uiState.update { it.copy(zoomScale = 1f, panOffset = androidx.compose.ui.geometry.Offset.Zero) }
    }

    fun toggleAnimations() {
        _uiState.update { it.copy(animationsEnabled = !it.animationsEnabled) }
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

    fun onNewGameClick(
        boardSize: Int = 9, 
        playerType: PlayerType = PlayerType.HUMAN_VS_AI, 
        handicap: Int = 0, 
        aiDifficulty: AIDifficulty? = AIDifficulty.EASY
    ) {
        aiJob?.cancel()
        gameState = GameState.newGame(boardSize)
        gameHistory.clear()
        gameHistory.add(gameState)
        historyIndex = 0
        
        // Configure agents based on player type and difficulty
        when (playerType) {
            PlayerType.HUMAN_VS_AI -> {
                blackAgent = null // Human plays black
                whiteAgent = aiDifficulty?.let { AIFactory.createAgent(it) } ?: RandomBot()
            }
            PlayerType.HUMAN_VS_HUMAN -> {
                blackAgent = null // Human plays black
                whiteAgent = null // Human plays white
            }
            PlayerType.AI_VS_AI -> {
                blackAgent = aiDifficulty?.let { AIFactory.createAgent(it) } ?: RandomBot()
                whiteAgent = aiDifficulty?.let { AIFactory.createAgent(it) } ?: RandomBot()
            }
        }
        
        updateUiState()
        checkAndMakeAiMove()
    }

    private fun applyMove(move: Move) {
        if (gameState.isValidMove(move)) {
            val previousState = gameState
            val movingPlayer = previousState.nextPlayer
            
            gameState = gameState.applyMove(move)
            
            // Check for captures
            val previousBoard = previousState.board
            val currentBoard = gameState.board
            var capturesOccurred = false
            
            for (row in 1..currentBoard.numRows) {
                for (col in 1..currentBoard.numCols) {
                    val point = Point(row, col)
                    val previousStone = previousBoard.get(point)
                    val currentStone = currentBoard.get(point)
                    
                    if (previousStone == movingPlayer.other() && currentStone == null) {
                        capturesOccurred = true
                        break
                    }
                }
                if (capturesOccurred) break
            }
            
            // Provide feedback based on move type
            when (val action = move.action) {
                is MoveAction.Play -> {
                    feedbackManager.onStonePlace(movingPlayer)
                    if (capturesOccurred) {
                        // Delay capture feedback slightly to not overlap with placement
                        viewModelScope.launch {
                            delay(100)
                            feedbackManager.onStoneCapture()
                        }
                    }
                }
                is MoveAction.Pass -> {
                    feedbackManager.onButtonClick()
                }
                is MoveAction.Resign -> {
                    feedbackManager.onButtonClick()
                }
            }
            
            if (historyIndex < gameHistory.size - 1) {
                gameHistory.subList(historyIndex + 1, gameHistory.size).clear()
            }
            gameHistory.add(gameState)
            historyIndex++
            moveCount++
            
            // Auto-save the move
            viewModelScope.launch {
                autoSaveMove(move, previousState.nextPlayer)
            }
            
            updateUiState()
            
            if (gameState.isOver()) {
                // Determine winner and provide appropriate feedback
                val winner = determineWinner()
                val humanPlayer = when {
                    blackAgent == null -> Player.Black
                    whiteAgent == null -> Player.White
                    else -> null // AI vs AI game
                }
                
                if (humanPlayer != null) {
                    if (winner == humanPlayer) {
                        feedbackManager.onGameWin()
                    } else if (winner != null) {
                        feedbackManager.onGameLose()
                    }
                }
                
                saveGameCompletion()
            } else {
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
            canRedo = historyIndex < gameHistory.size - 1,
            hoverPoint = _uiState.value.hoverPoint,
            invalidMoveAttempt = _uiState.value.invalidMoveAttempt,
            zoomScale = _uiState.value.zoomScale,
            panOffset = _uiState.value.panOffset,
            animatingStones = _uiState.value.animatingStones,
            capturedStones = _uiState.value.capturedStones,
            animationsEnabled = _uiState.value.animationsEnabled
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

    // Save/Load functionality
    fun saveCurrentGame(blackPlayerName: String = "Human", whitePlayerName: String = "AI") {
        viewModelScope.launch {
            try {
                if (currentGameId == null) {
                    currentGameId = repository.saveNewGame(gameState, blackPlayerName, whitePlayerName)
                }
                repository.updateGameState(
                    currentGameId!!,
                    gameState,
                    System.currentTimeMillis() - gameStartTime
                )
            } catch (e: Exception) {
                // Handle save error
            }
        }
    }

    fun loadGame(gameId: Long) {
        viewModelScope.launch {
            try {
                val loadedGameState = repository.loadGameState(gameId)
                if (loadedGameState != null) {
                    currentGameId = gameId
                    gameState = loadedGameState
                    gameHistory.clear()
                    gameHistory.add(gameState)
                    historyIndex = 0
                    moveCount = gameHistory.size - 1
                    updateUiState()
                    checkAndMakeAiMove()
                }
            } catch (e: Exception) {
                // Handle load error
            }
        }
    }

    private suspend fun autoSaveMove(move: Move, player: Player) {
        try {
            if (currentGameId == null) {
                currentGameId = repository.saveNewGame(gameState, "Human", "AI")
            }
            
            // Calculate captured stones
            val capturedStones = when (val action = move.action) {
                is MoveAction.Play -> calculateCapturedStones(action.point, player)
                else -> emptySet()
            }
            
            repository.saveMove(currentGameId!!, move, player, moveCount, capturedStones)
        } catch (e: Exception) {
            // Handle save error silently for auto-save
        }
    }

    private fun calculateCapturedStones(movePoint: Point, player: Player): Set<Point> {
        // This is a simplified implementation
        // In reality, you'd need to analyze the board state change
        val capturedStones = mutableSetOf<Point>()
        
        // Check adjacent points for captured opponent groups
        val opponents = listOfNotNull(
            Point(movePoint.row - 1, movePoint.col),
            Point(movePoint.row + 1, movePoint.col),
            Point(movePoint.row, movePoint.col - 1),
            Point(movePoint.row, movePoint.col + 1)
        ).filter { point ->
            point.row in 1..gameState.board.numRows && 
            point.col in 1..gameState.board.numCols &&
            gameState.board.get(point) == player.other()
        }
        
        // For each opponent stone, check if its group has no liberties
        // This is a simplified check - real implementation would be more complex
        return capturedStones
    }

    private fun saveGameCompletion() {
        viewModelScope.launch {
            currentGameId?.let { gameId ->
                val winner = determineWinner()
                val blackScore = calculateScore(Player.Black)
                val whiteScore = calculateScore(Player.White)
                repository.completeGame(gameId, winner, blackScore, whiteScore)
            }
        }
    }

    private fun calculateScore(player: Player): Float {
        // Simple stone counting for now
        return gameState.board.grid.values.count { it.color == player }.toFloat()
    }

    // Feedback settings methods
    fun setSoundEnabled(enabled: Boolean) {
        feedbackManager.setSoundEnabled(enabled)
    }
    
    fun setHapticEnabled(enabled: Boolean) {
        feedbackManager.setHapticEnabled(enabled)
    }
    
    fun setMasterVolume(volume: Float) {
        feedbackManager.setMasterVolume(volume)
    }
    
    fun setHapticIntensity(intensity: com.github.a2kaido.go.android.haptic.HapticIntensity) {
        feedbackManager.setHapticIntensity(intensity)
    }
    
    fun isSoundEnabled(): Boolean = feedbackManager.isSoundEnabled()
    fun isHapticEnabled(): Boolean = feedbackManager.isHapticEnabled()
    fun getMasterVolume(): Float = feedbackManager.getMasterVolume()
    fun getHapticIntensity(): com.github.a2kaido.go.android.haptic.HapticIntensity = feedbackManager.getHapticIntensity()
    fun isHapticSupported(): Boolean = feedbackManager.isHapticSupported()

    override fun onCleared() {
        super.onCleared()
        aiJob?.cancel()
        feedbackManager.release()
    }
}