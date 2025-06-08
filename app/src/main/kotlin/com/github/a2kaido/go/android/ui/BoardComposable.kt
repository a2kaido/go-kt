package com.github.a2kaido.go.android.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.a2kaido.go.android.ui.theme.GoGameTheme
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point

@Composable
fun BoardComposable(
    boardState: Map<Point, Player>,
    boardSize: Int,
    lastMove: Point? = null,
    modifier: Modifier = Modifier,
    showCoordinates: Boolean = true,
    onCellClick: (Int, Int) -> Unit = { _, _ -> },
    onCellHover: (Int, Int) -> Unit = { _, _ -> },
    onHoverExit: () -> Unit = {},
    enabled: Boolean = true,
    currentPlayer: Player = Player.Black,
    hoverPoint: Point? = null,
    invalidMoveAttempt: Point? = null,
    onZoomPanChange: (Float, Offset) -> Unit = { _, _ -> },
    zoomScale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    animatingStones: Set<Point> = emptySet(),
    capturedStones: Set<Point> = emptySet(),
    animationsEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "InvalidMoveShake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (invalidMoveAttempt != null) 10f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShakeAnimation"
    )
    
    // Animation for stone placement
    val stoneAnimations = remember { mutableMapOf<Point, Animatable<Float, AnimationVector1D>>() }
    
    // Animate new stones
    LaunchedEffect(boardState.keys) {
        if (animationsEnabled) {
            boardState.keys.forEach { point ->
                if (point !in stoneAnimations) {
                    val animatable = Animatable(0f)
                    stoneAnimations[point] = animatable
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
    }
    
    // Clean up animations for removed stones
    LaunchedEffect(boardState.keys) {
        stoneAnimations.keys.removeAll { point ->
            point !in boardState
        }
    }
    
    // Board colors
    val boardColor = Color(0xFFD4A574) // Wood grain color
    val lineColor = Color(0xFF2D1810)
    val starPointColor = Color(0xFF2D1810)
    val coordinateColor = Color(0xFF2D1810)
    
    // Stone colors
    val blackStoneColor = Color(0xFF1A1A1A)
    val whiteStoneColor = Color(0xFFF5F5F5)
    val stoneShadowColor = Color(0x40000000)
    
    // State for gesture handling
    var scale by remember { mutableStateOf(zoomScale) }
    var offset by remember { mutableStateOf(panOffset) }
    
    // Update scale and offset when external state changes
    LaunchedEffect(zoomScale, panOffset) {
        scale = zoomScale
        offset = panOffset
    }
    
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(boardSize > 9, enabled) {
                if (boardSize > 9) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 3f)
                        val newOffset = if (newScale > 1f) {
                            offset + pan
                        } else {
                            Offset.Zero
                        }
                        scale = newScale
                        offset = newOffset
                        onZoomPanChange(newScale, newOffset)
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (enabled) {
                                val boardSizePx = size.width.coerceAtMost(size.height).toFloat()
                                val coordinateOffset = if (showCoordinates) 30.dp.toPx() else 0f
                                val gridSize = boardSizePx - (coordinateOffset * 2)
                                val cellSize = gridSize / (boardSize - 1)
                                
                                // Adjust for zoom and pan
                                val adjustedOffset = Offset(
                                    (tapOffset.x - offset.x) / scale,
                                    (tapOffset.y - offset.y) / scale
                                )
                                
                                val col = ((adjustedOffset.x - coordinateOffset + cellSize / 2) / cellSize).toInt() + 1
                                val row = ((adjustedOffset.y - coordinateOffset + cellSize / 2) / cellSize).toInt() + 1
                                
                                if (row in 1..boardSize && col in 1..boardSize) {
                                    onCellClick(row, col)
                                }
                            }
                        }
                    )
                }
                .pointerInput(enabled, scale, offset) {
                    var currentPosition = Offset.Zero
                    detectDragGestures(
                        onDragStart = { dragOffset ->
                            if (enabled) {
                                currentPosition = dragOffset
                                val boardSizePx = size.width.coerceAtMost(size.height).toFloat()
                                val coordinateOffset = if (showCoordinates) 30.dp.toPx() else 0f
                                val gridSize = boardSizePx - (coordinateOffset * 2)
                                val cellSize = gridSize / (boardSize - 1)
                                
                                // Adjust for zoom and pan
                                val adjustedOffset = Offset(
                                    (dragOffset.x - offset.x) / scale,
                                    (dragOffset.y - offset.y) / scale
                                )
                                
                                val col = ((adjustedOffset.x - coordinateOffset + cellSize / 2) / cellSize).toInt() + 1
                                val row = ((adjustedOffset.y - coordinateOffset + cellSize / 2) / cellSize).toInt() + 1
                                
                                if (row in 1..boardSize && col in 1..boardSize) {
                                    onCellHover(row, col)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            if (enabled) {
                                currentPosition = change.position
                                val boardSizePx = size.width.coerceAtMost(size.height).toFloat()
                                val coordinateOffset = if (showCoordinates) 30.dp.toPx() else 0f
                                val gridSize = boardSizePx - (coordinateOffset * 2)
                                val cellSize = gridSize / (boardSize - 1)
                                
                                // Adjust for zoom and pan
                                val adjustedOffset = Offset(
                                    (currentPosition.x - offset.x) / scale,
                                    (currentPosition.y - offset.y) / scale
                                )
                                
                                val col = ((adjustedOffset.x - coordinateOffset + cellSize / 2) / cellSize).toInt() + 1
                                val row = ((adjustedOffset.y - coordinateOffset + cellSize / 2) / cellSize).toInt() + 1
                                
                                if (row in 1..boardSize && col in 1..boardSize) {
                                    onCellHover(row, col)
                                } else {
                                    onHoverExit()
                                }
                            }
                        },
                        onDragEnd = {
                            onHoverExit()
                        },
                        onDragCancel = {
                            onHoverExit()
                        }
                    )
                }
        ) {
            val boardSizePx = minOf(size.width, size.height)
            val coordinateOffset = if (showCoordinates) 30.dp.toPx() else 0f
            val gridSize = boardSizePx - (coordinateOffset * 2)
            val cellSize = gridSize / (boardSize - 1)
            val gridOffset = Offset(coordinateOffset, coordinateOffset)
            
            // Draw grid lines
            drawGridLines(
                numRows = boardSize,
                numCols = boardSize,
                cellSize = cellSize,
                gridOffset = gridOffset,
                lineColor = lineColor
            )
            
            // Draw star points
            drawStarPoints(
                numRows = boardSize,
                numCols = boardSize,
                cellSize = cellSize,
                gridOffset = gridOffset,
                starPointColor = starPointColor
            )
            
            // Draw coordinates
            if (showCoordinates) {
                drawCoordinates(
                    numRows = boardSize,
                    numCols = boardSize,
                    cellSize = cellSize,
                    gridOffset = gridOffset,
                    coordinateColor = coordinateColor,
                    textMeasurer = textMeasurer,
                    density = density
                )
            }
            
            // Draw stones
            drawStones(
                boardState = boardState,
                cellSize = cellSize,
                gridOffset = gridOffset,
                blackStoneColor = blackStoneColor,
                whiteStoneColor = whiteStoneColor,
                stoneShadowColor = stoneShadowColor,
                lastMove = lastMove,
                stoneAnimations = stoneAnimations,
                animationsEnabled = animationsEnabled
            )
            
            // Draw ghost stone with hover animation
            if (hoverPoint != null && enabled) {
                drawGhostStone(
                    point = hoverPoint,
                    player = currentPlayer,
                    cellSize = cellSize,
                    gridOffset = gridOffset,
                    blackStoneColor = blackStoneColor,
                    whiteStoneColor = whiteStoneColor,
                    animationsEnabled = animationsEnabled
                )
            }
            
            // Draw invalid move indicator with shake animation
            if (invalidMoveAttempt != null) {
                drawInvalidMoveIndicator(
                    point = invalidMoveAttempt,
                    cellSize = cellSize,
                    gridOffset = gridOffset,
                    shakeOffset = if (animationsEnabled) shakeOffset else 0f
                )
            }
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
    boardState: Map<Point, Player>,
    cellSize: Float,
    gridOffset: Offset,
    blackStoneColor: Color,
    whiteStoneColor: Color,
    stoneShadowColor: Color,
    lastMove: Point?,
    stoneAnimations: Map<Point, Animatable<Float, AnimationVector1D>>,
    animationsEnabled: Boolean
) {
    val stoneRadius = cellSize * 0.4f
    val shadowOffset = 2.dp.toPx()
    
    boardState.forEach { (point, player) ->
        val x = gridOffset.x + (point.col - 1) * cellSize
        val y = gridOffset.y + (point.row - 1) * cellSize
        val center = Offset(x, y)
        
        // Get animation scale
        val animationScale = if (animationsEnabled) {
            stoneAnimations[point]?.value ?: 1f
        } else {
            1f
        }
        
        val animatedRadius = stoneRadius * animationScale
        val animatedShadowOffset = shadowOffset * animationScale
        
        // Draw shadow with animation
        if (animationScale > 0f) {
            drawCircle(
                color = stoneShadowColor.copy(alpha = stoneShadowColor.alpha * animationScale),
                radius = animatedRadius,
                center = center + Offset(animatedShadowOffset, animatedShadowOffset)
            )
        }
        
        // Draw stone with gradient and animation
        if (animationScale > 0f) {
            val stoneGradient = when (player) {
                Player.Black -> Brush.radialGradient(
                    colors = listOf(
                        blackStoneColor.copy(alpha = 0.8f * animationScale),
                        blackStoneColor.copy(alpha = animationScale)
                    ),
                    center = center - Offset(animatedRadius * 0.3f, animatedRadius * 0.3f),
                    radius = animatedRadius * 1.2f
                )
                Player.White -> Brush.radialGradient(
                    colors = listOf(
                        whiteStoneColor.copy(alpha = animationScale),
                        whiteStoneColor.copy(alpha = 0.9f * animationScale)
                    ),
                    center = center - Offset(animatedRadius * 0.3f, animatedRadius * 0.3f),
                    radius = animatedRadius * 1.2f
                )
            }
            
            drawCircle(
                brush = stoneGradient,
                radius = animatedRadius,
                center = center
            )
            
            // Draw stone border
            drawCircle(
                color = if (player == Player.White) Color(0xFFCCCCCC).copy(alpha = animationScale) else Color(0xFF444444).copy(alpha = animationScale),
                radius = animatedRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Mark last move with enhanced animation
            if (point == lastMove) {
                val markColor = if (player == Player.Black) Color.White else Color.Black
                
                // Pulsing effect for last move
                val pulseScale = if (animationsEnabled) {
                    1f + 0.2f * kotlin.math.sin(System.currentTimeMillis().toFloat() / 300f)
                } else {
                    1f
                }
                
                drawCircle(
                    color = markColor.copy(alpha = animationScale),
                    radius = animatedRadius * 0.3f * pulseScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx() * animationScale)
                )
            }
        }
    }
}

private fun DrawScope.drawGhostStone(
    point: Point,
    player: Player,
    cellSize: Float,
    gridOffset: Offset,
    blackStoneColor: Color,
    whiteStoneColor: Color,
    animationsEnabled: Boolean = true
) {
    val x = gridOffset.x + (point.col - 1) * cellSize
    val y = gridOffset.y + (point.row - 1) * cellSize
    val center = Offset(x, y)
    val stoneRadius = cellSize * 0.4f
    
    // Pulsing effect for ghost stone
    val pulseAlpha = if (animationsEnabled) {
        0.4f + 0.2f * kotlin.math.sin(System.currentTimeMillis().toFloat() / 400f)
    } else {
        0.5f
    }
    
    // Draw semi-transparent stone with pulse
    val ghostColor = when (player) {
        Player.Black -> blackStoneColor.copy(alpha = pulseAlpha)
        Player.White -> whiteStoneColor.copy(alpha = pulseAlpha)
    }
    
    drawCircle(
        color = ghostColor,
        radius = stoneRadius,
        center = center
    )
    
    // Draw border with pulse
    drawCircle(
        color = if (player == Player.White) Color(0xFFCCCCCC).copy(alpha = pulseAlpha) else Color(0xFF444444).copy(alpha = pulseAlpha),
        radius = stoneRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawInvalidMoveIndicator(
    point: Point,
    cellSize: Float,
    gridOffset: Offset,
    shakeOffset: Float = 0f
) {
    val x = gridOffset.x + (point.col - 1) * cellSize + shakeOffset
    val y = gridOffset.y + (point.row - 1) * cellSize
    val center = Offset(x, y)
    val crossSize = cellSize * 0.3f
    
    // Draw red X with shake animation
    val strokeWidth = 3.dp.toPx()
    drawLine(
        color = Color.Red,
        start = center + Offset(-crossSize, -crossSize),
        end = center + Offset(crossSize, crossSize),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.Red,
        start = center + Offset(-crossSize, crossSize),
        end = center + Offset(crossSize, -crossSize),
        strokeWidth = strokeWidth
    )
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
        val boardState = mapOf(
            Point(3, 3) to Player.Black,
            Point(4, 4) to Player.White,
            Point(5, 5) to Player.Black
        )
        
        BoardComposable(
            boardState = boardState,
            boardSize = 9,
            lastMove = Point(5, 5),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            animationsEnabled = true
        )
    }
}