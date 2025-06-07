package com.github.a2kaido.go.android.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.github.a2kaido.go.android.data.entity.MoveRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MoveRecordDao {
    @Query("SELECT * FROM move_records WHERE gameId = :gameId ORDER BY moveNumber ASC")
    suspend fun getMovesForGame(gameId: Long): List<MoveRecord>

    @Query("SELECT * FROM move_records WHERE gameId = :gameId ORDER BY moveNumber ASC")
    fun getMovesForGameFlow(gameId: Long): Flow<List<MoveRecord>>

    @Insert
    suspend fun insertMove(move: MoveRecord): Long

    @Insert
    suspend fun insertMoves(moves: List<MoveRecord>)

    @Delete
    suspend fun deleteMove(move: MoveRecord)

    @Query("DELETE FROM move_records WHERE gameId = :gameId")
    suspend fun deleteMovesForGame(gameId: Long)

    @Query("DELETE FROM move_records WHERE gameId = :gameId AND moveNumber > :moveNumber")
    suspend fun deleteMovesAfter(gameId: Long, moveNumber: Int)

    @Query("SELECT COUNT(*) FROM move_records WHERE gameId = :gameId")
    suspend fun getMoveCount(gameId: Long): Int
}