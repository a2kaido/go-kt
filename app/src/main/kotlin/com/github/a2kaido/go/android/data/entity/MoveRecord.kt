package com.github.a2kaido.go.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "move_records",
    foreignKeys = [
        ForeignKey(
            entity = SavedGame::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class MoveRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val moveNumber: Int,
    val playerColor: String, // "BLACK" or "WHITE"
    val moveType: String, // "PLAY", "PASS", "RESIGN"
    val row: Int? = null, // null for PASS/RESIGN
    val col: Int? = null, // null for PASS/RESIGN
    val capturedStones: String? = null, // JSON array of captured positions
    val timestamp: Long
)