package com.github.a2kaido.go.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.a2kaido.go.android.ui.BoardComposable
import com.github.a2kaido.go.android.ui.theme.GoGameTheme
import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GoGameScreen()
                }
            }
        }
    }
}

@Composable
fun GoGameScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Go Game",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val sampleBoard = createSampleBoard()
        
        BoardComposable(
            board = sampleBoard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

private fun createSampleBoard(): Board {
    val board = Board(9, 9, mutableMapOf())
    
    // Add some sample stones to demonstrate the board
    board.placeStone(Player.Black, Point(3, 3))
    board.placeStone(Player.White, Point(4, 4))
    board.placeStone(Player.Black, Point(5, 5))
    board.placeStone(Player.White, Point(6, 6))
    board.placeStone(Player.Black, Point(4, 3))
    board.placeStone(Player.White, Point(5, 4))
    board.placeStone(Player.Black, Point(7, 7))
    
    return board
}

@Preview(showBackground = true)
@Composable
fun GoGameScreenPreview() {
    GoGameTheme {
        GoGameScreen()
    }
}