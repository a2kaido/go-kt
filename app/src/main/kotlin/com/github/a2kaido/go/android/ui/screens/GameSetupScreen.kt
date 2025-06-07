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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameSetupScreen(
    onStartGame: (boardSize: Int, playerType: PlayerType, handicap: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBoardSize by remember { mutableIntStateOf(9) }
    var selectedPlayerType by remember { mutableStateOf(PlayerType.HUMAN_VS_AI) }
    var selectedHandicap by remember { mutableIntStateOf(0) }
    
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
                onClick = { onStartGame(selectedBoardSize, selectedPlayerType, selectedHandicap) },
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

enum class PlayerType {
    HUMAN_VS_AI,
    HUMAN_VS_HUMAN,
    AI_VS_AI
}