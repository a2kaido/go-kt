package com.github.a2kaido.go.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.a2kaido.go.android.navigation.NavigationRoutes
import com.github.a2kaido.go.android.ui.BoardComposable
import com.github.a2kaido.go.android.ui.FinishReason
import com.github.a2kaido.go.android.ui.GameStatus
import com.github.a2kaido.go.android.ui.GameUiState
import com.github.a2kaido.go.android.ui.screens.*
import com.github.a2kaido.go.android.ui.theme.GoGameTheme
import com.github.a2kaido.go.android.viewmodel.GameViewModel
import com.github.a2kaido.go.android.viewmodel.SavedGamesViewModel
import com.github.a2kaido.go.model.Player

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    private val savedGamesViewModel: SavedGamesViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val uiState by gameViewModel.uiState.collectAsState()
                    
                    NavHost(
                        navController = navController,
                        startDestination = NavigationRoutes.MainMenu.route
                    ) {
                        composable(NavigationRoutes.MainMenu.route) {
                            MainMenuScreen(
                                onNewGame = { navController.navigate(NavigationRoutes.GameSetup.route) },
                                onContinueGame = { navController.navigate(NavigationRoutes.Game.route) },
                                onSavedGames = { navController.navigate(NavigationRoutes.SavedGames.route) },
                                onSettings = { navController.navigate(NavigationRoutes.Settings.route) },
                                onAbout = { /* TODO: Implement about screen or dialog */ }
                            )
                        }
                        
                        composable(NavigationRoutes.GameSetup.route) {
                            GameSetupScreen(
                                onStartGame = { boardSize, playerType, handicap, aiDifficulty ->
                                    gameViewModel.onNewGameClick(boardSize, playerType, handicap, aiDifficulty)
                                    navController.navigate(NavigationRoutes.Game.route)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable(NavigationRoutes.Game.route) {
                            GoGameScreen(
                                uiState = uiState,
                                onCellClick = gameViewModel::onCellClick,
                                onCellHover = gameViewModel::onCellHover,
                                onHoverExit = gameViewModel::onHoverExit,
                                onZoomPanChange = gameViewModel::onZoomPanChange,
                                onResetZoomPan = gameViewModel::resetZoomPan,
                                onPassClick = gameViewModel::onPassClick,
                                onResignClick = gameViewModel::onResignClick,
                                onUndoClick = gameViewModel::onUndoClick,
                                onRedoClick = gameViewModel::onRedoClick,
                                onNewGameClick = gameViewModel::onNewGameClick,
                                onToggleAnimations = gameViewModel::toggleAnimations,
                                onBackToMenu = { navController.navigate(NavigationRoutes.MainMenu.route) },
                                onGameOver = { winner, score ->
                                    navController.navigate(NavigationRoutes.GameOver.createRoute(winner, score))
                                }
                            )
                        }
                        
                        composable(NavigationRoutes.SavedGames.route) {
                            val savedGames by savedGamesViewModel.savedGames.collectAsState()
                            
                            SavedGamesScreen(
                                savedGames = savedGames,
                                onGameSelect = { gameId ->
                                    gameViewModel.loadGame(gameId)
                                    navController.navigate(NavigationRoutes.Game.route)
                                },
                                onGameDelete = { gameId ->
                                    savedGamesViewModel.deleteGame(gameId)
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        
                        composable(NavigationRoutes.Settings.route) {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable(
                            route = NavigationRoutes.GameOver.route,
                            arguments = listOf(
                                navArgument("winner") { type = NavType.StringType },
                                navArgument("score") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val winner = backStackEntry.arguments?.getString("winner") ?: ""
                            val score = backStackEntry.arguments?.getString("score") ?: ""
                            
                            GameOverScreen(
                                winner = winner,
                                score = score,
                                onRematch = { 
                                    navController.navigate(NavigationRoutes.GameSetup.route) {
                                        popUpTo(NavigationRoutes.MainMenu.route)
                                    }
                                },
                                onBackToMenu = { 
                                    navController.navigate(NavigationRoutes.MainMenu.route) {
                                        popUpTo(NavigationRoutes.MainMenu.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoGameScreen(
    uiState: GameUiState,
    onCellClick: (Int, Int) -> Unit,
    onCellHover: (Int, Int) -> Unit = { _, _ -> },
    onHoverExit: () -> Unit = {},
    onZoomPanChange: (Float, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    onResetZoomPan: () -> Unit = {},
    onPassClick: () -> Unit,
    onResignClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onNewGameClick: (Int) -> Unit,
    onToggleAnimations: () -> Unit = {},
    onBackToMenu: (() -> Unit)? = null,
    onGameOver: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and status
        Text(
            text = "Go Game",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Game status
        when (val status = uiState.gameStatus) {
            is GameStatus.ONGOING -> {
                Text(
                    text = if (uiState.isThinking) {
                        "${uiState.currentPlayer} is thinking..."
                    } else {
                        "${uiState.currentPlayer}'s turn"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            is GameStatus.FINISHED -> {
                val winnerText = when (status.reason) {
                    FinishReason.RESIGNATION -> "${status.winner ?: "Draw"} wins by resignation!"
                    FinishReason.TWO_PASSES -> "Game ended. ${status.winner ?: "Draw"} wins!"
                    FinishReason.NORMAL -> "${status.winner ?: "Draw"} wins!"
                }
                Text(
                    text = winnerText,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Auto-navigate to game over screen if callback is provided
                LaunchedEffect(status) {
                    onGameOver?.invoke(
                        status.winner?.name ?: "Draw",
                        "Black: ${uiState.blackCaptures} captures, White: ${uiState.whiteCaptures} captures"
                    )
                }
            }
        }
        
        // Capture counts
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text("Black captures: ${uiState.blackCaptures}")
            Text("White captures: ${uiState.whiteCaptures}")
        }
        
        // Board
        BoardComposable(
            boardState = uiState.boardState,
            boardSize = uiState.boardSize,
            lastMove = uiState.lastMove,
            onCellClick = onCellClick,
            onCellHover = onCellHover,
            onHoverExit = onHoverExit,
            enabled = !uiState.isThinking && uiState.gameStatus is GameStatus.ONGOING,
            currentPlayer = uiState.currentPlayer,
            hoverPoint = uiState.hoverPoint,
            invalidMoveAttempt = uiState.invalidMoveAttempt,
            onZoomPanChange = onZoomPanChange,
            zoomScale = uiState.zoomScale,
            panOffset = uiState.panOffset,
            animatingStones = uiState.animatingStones,
            capturedStones = uiState.capturedStones,
            animationsEnabled = uiState.animationsEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reset zoom button (only show when zoomed)
        if (uiState.zoomScale > 1f && uiState.boardSize > 9) {
            Button(
                onClick = onResetZoomPan,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Reset Zoom")
            }
        }
        
        // Game controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPassClick,
                enabled = !uiState.isThinking && uiState.gameStatus is GameStatus.ONGOING
            ) {
                Text("Pass")
            }
            Button(
                onClick = onResignClick,
                enabled = !uiState.isThinking && uiState.gameStatus is GameStatus.ONGOING
            ) {
                Text("Resign")
            }
            Button(
                onClick = onUndoClick,
                enabled = uiState.canUndo && !uiState.isThinking
            ) {
                Text("Undo")
            }
            Button(
                onClick = onRedoClick,
                enabled = uiState.canRedo && !uiState.isThinking
            ) {
                Text("Redo")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // New game and menu buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onNewGameClick(9) },
                enabled = !uiState.isThinking
            ) {
                Text("New Game")
            }
            
            Button(
                onClick = onToggleAnimations,
                enabled = !uiState.isThinking
            ) {
                Text(if (uiState.animationsEnabled) "Disable Animations" else "Enable Animations")
            }
            
            if (onBackToMenu != null) {
                OutlinedButton(
                    onClick = onBackToMenu,
                    enabled = !uiState.isThinking
                ) {
                    Text("Menu")
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GoGameScreenPreview() {
    GoGameTheme {
        GoGameScreen(
            uiState = GameUiState(),
            onCellClick = { _, _ -> },
            onPassClick = { },
            onResignClick = { },
            onUndoClick = { },
            onRedoClick = { },
            onNewGameClick = { }
        )
    }
}