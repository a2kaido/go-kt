package com.github.a2kaido.go.android.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SavedGameWithMoves(
    @Embedded val savedGame: SavedGame,
    @Relation(
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val moves: List<MoveRecord>
)