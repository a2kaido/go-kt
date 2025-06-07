package com.github.a2kaido.go.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.a2kaido.go.android.ui.theme.GoGameTheme
import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point

@Composable
fun BoardComposable(
    board: Board,
    modifier: Modifier = Modifier,
    showCoordinates: Boolean = true,
    onIntersectionClick: ((Point) -> Unit)? = null
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Board colors
    val boardColor = Color(0xFFD4A574) // Wood grain color
    val lineColor = Color(0xFF2D1810)
    val starPointColor = Color(0xFF2D1810)
    val coordinateColor = Color(0xFF2D1810)
    
    // Stone colors
    val blackStoneColor = Color(0xFF1A1A1A)
    val whiteStoneColor = Color(0xFFF5F5F5)
    val stoneShadowColor = Color(0x40000000)
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        boardColor.copy(alpha = 0.95f),
                        boardColor.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val boardSize = size.minDimension
            val coordinateOffset = if (showCoordinates) 30.dp.toPx() else 0f
            val gridSize = boardSize - (coordinateOffset * 2)
            val cellSize = gridSize / (board.numCols - 1)
            val gridOffset = Offset(coordinateOffset, coordinateOffset)
            
            // Draw grid lines
            drawGridLines(
                numRows = board.numRows,
                numCols = board.numCols,
                cellSize = cellSize,
                gridOffset = gridOffset,
                lineColor = lineColor
            )
            
            // Draw star points
            drawStarPoints(
                numRows = board.numRows,
                numCols = board.numCols,
                cellSize = cellSize,
                gridOffset = gridOffset,
                starPointColor = starPointColor
            )
            
            // Draw coordinates
            if (showCoordinates) {
                drawCoordinates(
                    numRows = board.numRows,
                    numCols = board.numCols,
                    cellSize = cellSize,
                    gridOffset = gridOffset,
                    coordinateColor = coordinateColor,
                    textMeasurer = textMeasurer,
                    density = density
                )
            }
            
            // Draw stones
            drawStones(
                board = board,
                cellSize = cellSize,
                gridOffset = gridOffset,
                blackStoneColor = blackStoneColor,
                whiteStoneColor = whiteStoneColor,
                stoneShadowColor = stoneShadowColor
            )
        }
    }
}

private fun DrawScope.drawGridLines(
    numRows: Int,
    numCols: Int,
    cellSize: Float,
    gridOffset: Offset,
    lineColor: Color
) {
    val strokeWidth = 2.dp.toPx()
    
    // Horizontal lines
    for (row in 0 until numRows) {
        val y = gridOffset.y + row * cellSize
        drawLine(
            color = lineColor,
            start = Offset(gridOffset.x, y),
            end = Offset(gridOffset.x + (numCols - 1) * cellSize, y),
            strokeWidth = strokeWidth
        )
    }
    
    // Vertical lines
    for (col in 0 until numCols) {
        val x = gridOffset.x + col * cellSize
        drawLine(
            color = lineColor,
            start = Offset(x, gridOffset.y),
            end = Offset(x, gridOffset.y + (numRows - 1) * cellSize),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawStarPoints(
    numRows: Int,
    numCols: Int,
    cellSize: Float,
    gridOffset: Offset,
    starPointColor: Color
) {
    val starPointRadius = 4.dp.toPx()
    val starPoints = getStarPoints(numRows, numCols)
    
    starPoints.forEach { point ->
        val x = gridOffset.x + (point.col - 1) * cellSize
        val y = gridOffset.y + (point.row - 1) * cellSize
        drawCircle(
            color = starPointColor,
            radius = starPointRadius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawCoordinates(
    numRows: Int,
    numCols: Int,
    cellSize: Float,
    gridOffset: Offset,
    coordinateColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density
) {
    val textStyle = TextStyle(
        color = coordinateColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
    
    // Column labels (A-T)
    for (col in 0 until numCols) {
        val letter = ('A' + col).toString()
        val x = gridOffset.x + col * cellSize
        val textLayout = textMeasurer.measure(letter, textStyle)
        
        // Top
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                x - textLayout.size.width / 2,
                gridOffset.y - 25.dp.toPx()
            )
        )
        
        // Bottom
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                x - textLayout.size.width / 2,
                gridOffset.y + (numRows - 1) * cellSize + 10.dp.toPx()
            )
        )
    }
    
    // Row labels (1-19)
    for (row in 0 until numRows) {
        val number = (numRows - row).toString()
        val y = gridOffset.y + row * cellSize
        val textLayout = textMeasurer.measure(number, textStyle)
        
        // Left
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                gridOffset.x - 25.dp.toPx(),
                y - textLayout.size.height / 2
            )
        )
        
        // Right
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                gridOffset.x + (numCols - 1) * cellSize + 10.dp.toPx(),
                y - textLayout.size.height / 2
            )
        )
    }
}

private fun DrawScope.drawStones(
    board: Board,
    cellSize: Float,
    gridOffset: Offset,
    blackStoneColor: Color,
    whiteStoneColor: Color,
    stoneShadowColor: Color
) {
    val stoneRadius = cellSize * 0.4f
    val shadowOffset = 2.dp.toPx()
    
    board.grid.values.forEach { goString ->
        val stoneColor = when (goString.color) {
            Player.Black -> blackStoneColor
            Player.White -> whiteStoneColor
        }
        
        goString.stones.forEach { point ->
            val x = gridOffset.x + (point.col - 1) * cellSize
            val y = gridOffset.y + (point.row - 1) * cellSize
            val center = Offset(x, y)
            
            // Draw shadow
            drawCircle(
                color = stoneShadowColor,
                radius = stoneRadius,
                center = center + Offset(shadowOffset, shadowOffset)
            )
            
            // Draw stone with gradient
            val stoneGradient = when (goString.color) {
                Player.Black -> Brush.radialGradient(
                    colors = listOf(
                        blackStoneColor.copy(alpha = 0.8f),
                        blackStoneColor
                    ),
                    center = center - Offset(stoneRadius * 0.3f, stoneRadius * 0.3f),
                    radius = stoneRadius * 1.2f
                )
                Player.White -> Brush.radialGradient(
                    colors = listOf(
                        whiteStoneColor,
                        whiteStoneColor.copy(alpha = 0.9f)
                    ),
                    center = center - Offset(stoneRadius * 0.3f, stoneRadius * 0.3f),
                    radius = stoneRadius * 1.2f
                )
            }
            
            drawCircle(
                brush = stoneGradient,
                radius = stoneRadius,
                center = center
            )
            
            // Draw stone border
            drawCircle(
                color = if (goString.color == Player.White) Color(0xFFCCCCCC) else Color(0xFF444444),
                radius = stoneRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun getStarPoints(numRows: Int, numCols: Int): List<Point> {
    return when (numRows) {
        9 -> listOf(
            Point(3, 3), Point(3, 7),
            Point(5, 5),
            Point(7, 3), Point(7, 7)
        )
        13 -> listOf(
            Point(4, 4), Point(4, 7), Point(4, 10),
            Point(7, 4), Point(7, 7), Point(7, 10),
            Point(10, 4), Point(10, 7), Point(10, 10)
        )
        19 -> listOf(
            Point(4, 4), Point(4, 10), Point(4, 16),
            Point(10, 4), Point(10, 10), Point(10, 16),
            Point(16, 4), Point(16, 10), Point(16, 16)
        )
        else -> emptyList()
    }
}

@Preview(showBackground = true)
@Composable
fun BoardComposablePreview() {
    GoGameTheme {
        val board = Board(9, 9, mutableMapOf())
        // Add some sample stones for preview
        board.placeStone(Player.Black, Point(3, 3))
        board.placeStone(Player.White, Point(4, 4))
        board.placeStone(Player.Black, Point(5, 5))
        
        BoardComposable(
            board = board,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}