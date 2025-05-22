package com.github.a2kaido.go

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.a2kaido.go.sgf.SgfNode
import com.github.a2kaido.go.sgf.parseSgf
import com.github.a2kaido.go.ui.GoBoardView
import com.github.a2kaido.go.ui.KifuViewModel
import java.io.InputStreamReader

@Composable
fun AppUI(kifuViewModel: KifuViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Move Information Text
        Text(
            text = if (kifuViewModel.currentMoveDisplayNumber == 0 && kifuViewModel.totalPlayableMoves > 0) {
                "Initial Setup / ${kifuViewModel.totalPlayableMoves} moves"
            } else if (kifuViewModel.totalPlayableMoves == 0) {
                 "Setup Only / No Moves"
            }
            else {
                "Move ${kifuViewModel.currentMoveDisplayNumber} / ${kifuViewModel.totalPlayableMoves}"
            },
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Go Board
        Box(modifier = Modifier.weight(1f)) { // Make board take available space
             GoBoardView(board = kifuViewModel.currentBoard, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comment Box
        Text(
            text = "Comment: ${kifuViewModel.currentComment ?: "N/A"}",
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp), // Fixed height for comment box
            textAlign = TextAlign.Center
        )

        // Navigation Buttons - Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { kifuViewModel.goToBoardSetup() },
                enabled = kifuViewModel.hasSgfNodes && !(kifuViewModel.currentSgfNodeIndex == 0 && kifuViewModel.currentMoveDisplayNumber == 0)
            ) {
                Text("Setup")
            }
            Button(
                onClick = { kifuViewModel.goToFirstPlayableMove() },
                // Enabled if there are playable moves and not currently at the first move.
                // Or if at setup (display 0) and there are moves.
                enabled = kifuViewModel.totalPlayableMoves > 0 && 
                          (kifuViewModel.currentMoveDisplayNumber != 1 || (kifuViewModel.currentMoveDisplayNumber == 0 && kifuViewModel.currentSgfNodeIndex ==0) )
            ) {
                Text("First")
            }
            Button(
                onClick = { kifuViewModel.previousMove() },
                enabled = kifuViewModel.hasSgfNodes && kifuViewModel.currentSgfNodeIndex >= 0 
            ) {
                Text("Prev")
            }
            Button(
                onClick = { kifuViewModel.nextMove() },
                enabled = kifuViewModel.hasSgfNodes && !kifuViewModel.isAtEndOfKifu
            ) {
                Text("Next")
            }
            Button(
                onClick = { kifuViewModel.goToLastMove() },
                enabled = kifuViewModel.hasSgfNodes && !kifuViewModel.isAtEndOfKifu // Simplification: enable if not at the very last SGF node. goToLastMove itself finds the last actual move.
            ) {
                Text("Last")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Navigation Buttons - Row 2 (Optional: for jump to specific move, etc.)
        // For now, the "Reset (Empty)" is useful.
         Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
         ) {
            Button(onClick = { kifuViewModel.goToMove(-1) }) {
                Text("Empty Board")
            }
         }
    }
}


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Go Kifu Viewer",
        state = rememberWindowState(width = 550.dp, height = 750.dp) // Adjusted for more controls
    ) {
        val sgfString = try {
            val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("sample.sgf")
            if (resourceStream == null) {
                System.err.println("Error: sample.sgf not found in resources.")
                "" // Return empty string or handle error appropriately
            } else {
                InputStreamReader(resourceStream).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Error loading sample.sgf: ${e.message}")
            "" // Return empty string or handle error appropriately
        }

        val parsedNodes: List<SgfNode> = if (sgfString.isNotBlank()) {
            try {
                parseSgf(sgfString)
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Error parsing SGF string: ${e.message}")
                emptyList() // Handle parsing error by providing empty nodes
            }
        } else {
            emptyList()
        }
        
        // Add KifuViewModel to access its properties, ensure parsedNodes is the key for remember
        val kifuViewModel = remember(parsedNodes) { KifuViewModel(parsedNodes) }

        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppUI(kifuViewModel = kifuViewModel)
            }
        }
    }
}
