package com.github.a2kaido.go.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.a2kaido.go.agent.AIDifficulty

@Composable
fun GameSetupScreen(
    onStartGame: (boardSize: Int, playerType: PlayerType, handicap: Int, aiDifficulty: AIDifficulty?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBoardSize by remember { mutableIntStateOf(9) }
    var selectedPlayerType by remember { mutableStateOf(PlayerType.HUMAN_VS_AI) }
    var selectedHandicap by remember { mutableIntStateOf(0) }
    var selectedAIDifficulty by remember { mutableStateOf(AIDifficulty.EASY) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Game Setup",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Board Size",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(modifier = Modifier.selectableGroup()) {
                    BoardSizeOption(
                        size = 9,
                        description = "9×9 (Beginner)",
                        selected = selectedBoardSize == 9,
                        onSelect = { selectedBoardSize = 9 }
                    )
                    BoardSizeOption(
                        size = 13,
                        description = "13×13 (Intermediate)",
                        selected = selectedBoardSize == 13,
                        onSelect = { selectedBoardSize = 13 }
                    )
                    BoardSizeOption(
                        size = 19,
                        description = "19×19 (Standard)",
                        selected = selectedBoardSize == 19,
                        onSelect = { selectedBoardSize = 19 }
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Player Type",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(modifier = Modifier.selectableGroup()) {
                    PlayerTypeOption(
                        type = PlayerType.HUMAN_VS_AI,
                        description = "Human vs AI",
                        selected = selectedPlayerType == PlayerType.HUMAN_VS_AI,
                        onSelect = { selectedPlayerType = PlayerType.HUMAN_VS_AI }
                    )
                    PlayerTypeOption(
                        type = PlayerType.HUMAN_VS_HUMAN,
                        description = "Human vs Human",
                        selected = selectedPlayerType == PlayerType.HUMAN_VS_HUMAN,
                        onSelect = { selectedPlayerType = PlayerType.HUMAN_VS_HUMAN }
                    )
                    PlayerTypeOption(
                        type = PlayerType.AI_VS_AI,
                        description = "AI vs AI",
                        selected = selectedPlayerType == PlayerType.AI_VS_AI,
                        onSelect = { selectedPlayerType = PlayerType.AI_VS_AI }
                    )
                }
            }
        }
        
        // AI Difficulty section - only show when AI is involved
        if (selectedPlayerType != PlayerType.HUMAN_VS_HUMAN) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI Difficulty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Column(modifier = Modifier.selectableGroup()) {
                        AIDifficultyOption(
                            difficulty = AIDifficulty.BEGINNER,
                            selected = selectedAIDifficulty == AIDifficulty.BEGINNER,
                            onSelect = { selectedAIDifficulty = AIDifficulty.BEGINNER }
                        )
                        AIDifficultyOption(
                            difficulty = AIDifficulty.EASY,
                            selected = selectedAIDifficulty == AIDifficulty.EASY,
                            onSelect = { selectedAIDifficulty = AIDifficulty.EASY }
                        )
                        AIDifficultyOption(
                            difficulty = AIDifficulty.MEDIUM,
                            selected = selectedAIDifficulty == AIDifficulty.MEDIUM,
                            onSelect = { selectedAIDifficulty = AIDifficulty.MEDIUM }
                        )
                        AIDifficultyOption(
                            difficulty = AIDifficulty.HARD,
                            selected = selectedAIDifficulty == AIDifficulty.HARD,
                            onSelect = { selectedAIDifficulty = AIDifficulty.HARD }
                        )
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Handicap",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Stones: $selectedHandicap")
                    
                    Row {
                        OutlinedButton(
                            onClick = { if (selectedHandicap > 0) selectedHandicap-- },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        OutlinedButton(
                            onClick = { if (selectedHandicap < 9) selectedHandicap++ },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Back")
            }
            
            Button(
                onClick = { 
                    val aiDifficulty = if (selectedPlayerType == PlayerType.HUMAN_VS_HUMAN) null else selectedAIDifficulty
                    onStartGame(selectedBoardSize, selectedPlayerType, selectedHandicap, aiDifficulty) 
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Start Game")
            }
        }
    }
}

@Composable
private fun BoardSizeOption(
    size: Int,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = description)
    }
}

@Composable
private fun PlayerTypeOption(
    type: PlayerType,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = description)
    }
}

@Composable
private fun AIDifficultyOption(
    difficulty: AIDifficulty,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = difficulty.getDisplayName(),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = difficulty.getDescription(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class PlayerType {
    HUMAN_VS_AI,
    HUMAN_VS_HUMAN,
    AI_VS_AI
}