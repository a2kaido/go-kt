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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.a2kaido.go.android.ui.BoardComposable
import com.github.a2kaido.go.android.ui.FinishReason
import com.github.a2kaido.go.android.ui.GameStatus
import com.github.a2kaido.go.android.ui.GameUiState
import com.github.a2kaido.go.android.ui.theme.GoGameTheme
import com.github.a2kaido.go.android.viewmodel.GameViewModel
import com.github.a2kaido.go.model.Player

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
                    GoGameScreen(
                        uiState = uiState,
                        onCellClick = gameViewModel::onCellClick,
                        onPassClick = gameViewModel::onPassClick,
                        onResignClick = gameViewModel::onResignClick,
                        onUndoClick = gameViewModel::onUndoClick,
                        onRedoClick = gameViewModel::onRedoClick,
                        onNewGameClick = gameViewModel::onNewGameClick
                    )
                }
            }
        }
    }
}

@Composable
fun GoGameScreen(
    uiState: GameUiState,
    onCellClick: (Int, Int) -> Unit,
    onPassClick: () -> Unit,
    onResignClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onNewGameClick: (Int) -> Unit,
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
            enabled = !uiState.isThinking && uiState.gameStatus is GameStatus.ONGOING,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
        // New game button
        Button(
            onClick = { onNewGameClick(9) },
            enabled = !uiState.isThinking
        ) {
            Text("New Game")
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