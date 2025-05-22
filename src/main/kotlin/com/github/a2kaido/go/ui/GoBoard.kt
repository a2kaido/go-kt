package com.github.a2kaido.go.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
// Removed DrawScope import as it's not directly used after recent changes
// import androidx.compose.ui.graphics.drawscope.DrawScope 
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.a2kaido.go.model.Board // Corrected import
import com.github.a2kaido.go.model.Player  // Corrected import

// Define StoneColor enum
enum class StoneColor {
    BLACK, WHITE
}

@Composable
fun Stone(
    color: StoneColor,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val stoneColor = if (color == StoneColor.BLACK) Color.Black else Color.White
        drawCircle(
            color = stoneColor,
            radius = size.toPx() / 2,
            center = Offset(size.toPx() / 2, size.toPx() / 2)
        )
        // Optional: Add a border to white stones for better visibility
        if (color == StoneColor.WHITE) {
            drawCircle(
                color = Color.Black,
                radius = size.toPx() / 2,
                center = Offset(size.toPx() / 2, size.toPx() / 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = (size.toPx() * 0.05f).coerceAtLeast(1f))
            )
        }
    }
    // Incorrectly placed imports were here - REMOVED
}

@Composable
fun GoBoardView(board: Board, modifier: Modifier = Modifier) { // Parameter uses the imported Board
    val boardSize = board.size
    val boardDpSize = 400.dp // Define a fixed board size in Dp
    val lineSpacingDp = boardDpSize / (boardSize + 1)
    val stoneSizeDp = lineSpacingDp * 0.9f // Stones are slightly smaller than grid spacing

    Box(modifier = modifier.size(boardDpSize)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidthPx = size.width
            val canvasHeightPx = size.height
            val lineSpacingPx = lineSpacingDp.toPx()
            val borderMarginPx = lineSpacingPx // Margin from canvas edge to first line (also size of one grid cell)

            // Draw grid lines
            for (i in 0 until boardSize) {
                val currentPosPx = borderMarginPx + i * lineSpacingPx
                // Vertical lines
                drawLine(
                    color = Color.Black,
                    start = Offset(x = currentPosPx, y = borderMarginPx),
                    end = Offset(x = currentPosPx, y = canvasHeightPx - borderMarginPx)
                )
                // Horizontal lines
                drawLine(
                    color = Color.Black,
                    start = Offset(x = borderMarginPx, y = currentPosPx),
                    end = Offset(x = canvasWidthPx - borderMarginPx, y = currentPosPx)
                )
            }

            // Draw star points (hoshi)
            if (boardSize == 19) {
                val starPointSizePx = lineSpacingPx / 8 
                val starCoords = listOf(3, 9, 15) 
                starCoords.forEach { rowIdx ->
                    starCoords.forEach { colIdx ->
                        val centerX = borderMarginPx + colIdx * lineSpacingPx
                        val centerY = borderMarginPx + rowIdx * lineSpacingPx
                        drawCircle(
                            color = Color.Black,
                            radius = starPointSizePx,
                            center = Offset(x = centerX, y = centerY)
                        )
                    }
                }
            }
        }
        
        val stoneOffsetXDp = { col: Int -> lineSpacingDp + (lineSpacingDp * col) - stoneSizeDp / 2 }
        val stoneOffsetYDp = { row: Int -> lineSpacingDp + (lineSpacingDp * row) - stoneSizeDp / 2 }

        board.stones.forEach { (coords, player) -> // player is of type com.github.a2kaido.go.model.Player
            val (x, y) = coords 
            Stone(
                color = if (player == Player.BLACK) StoneColor.BLACK else StoneColor.WHITE,
                size = stoneSizeDp,
                modifier = Modifier.offset(x = stoneOffsetXDp(x), y = stoneOffsetYDp(y))
            )
        }
    }
}
