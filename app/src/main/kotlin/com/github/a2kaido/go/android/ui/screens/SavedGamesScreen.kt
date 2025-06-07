package com.github.a2kaido.go.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.a2kaido.go.android.data.entity.SavedGame
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedGamesScreen(
    savedGames: List<SavedGame>,
    onGameSelect: (Long) -> Unit,
    onGameDelete: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Saved Games") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (savedGames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No saved games",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Games will be automatically saved during play",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedGames, key = { it.id }) { game ->
                    SavedGameItem(
                        savedGame = game,
                        onGameSelect = { onGameSelect(game.id) },
                        onGameDelete = { onGameDelete(game.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedGameItem(
    savedGame: SavedGame,
    onGameSelect: () -> Unit,
    onGameDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGameSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Board thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                BoardThumbnail(
                    thumbnail = savedGame.thumbnail,
                    boardSize = savedGame.boardSize
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Game info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${savedGame.blackPlayerName} vs ${savedGame.whitePlayerName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${savedGame.boardSize}×${savedGame.boardSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    StatusChip(gameStatus = savedGame.gameStatus)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Moves: ${savedGame.moveCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = dateFormat.format(savedGame.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete game",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Game") },
            text = { Text("Are you sure you want to delete this saved game? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGameDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BoardThumbnail(
    thumbnail: String?,
    boardSize: Int
) {
    if (thumbnail != null) {
        // Simple grid representation
        val rows = thumbnail.split("|")
        Column(
            modifier = Modifier.size(40.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            rows.take(5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    row.take(5).forEach { cell ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    when (cell) {
                                        'B' -> Color.Black
                                        'W' -> Color.White
                                        else -> Color.Gray.copy(alpha = 0.3f)
                                    },
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = "${boardSize}×${boardSize}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusChip(gameStatus: String) {
    val (color, text) = when (gameStatus) {
        "IN_PROGRESS" -> MaterialTheme.colorScheme.primary to "In Progress"
        "COMPLETED" -> MaterialTheme.colorScheme.tertiary to "Completed"
        "RESIGNED" -> MaterialTheme.colorScheme.error to "Resigned"
        else -> MaterialTheme.colorScheme.outline to "Unknown"
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}