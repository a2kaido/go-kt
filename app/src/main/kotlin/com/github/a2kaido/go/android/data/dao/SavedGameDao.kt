package com.github.a2kaido.go.android.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.a2kaido.go.android.data.entity.SavedGame
import com.github.a2kaido.go.android.data.entity.SavedGameWithMoves
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games ORDER BY updatedAt DESC")
    fun getAllGames(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_games WHERE gameStatus = 'IN_PROGRESS' ORDER BY updatedAt DESC")
    fun getInProgressGames(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_games WHERE gameStatus = 'COMPLETED' ORDER BY updatedAt DESC")
    fun getCompletedGames(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_games WHERE id = :gameId")
    suspend fun getGameById(gameId: Long): SavedGame?

    @Transaction
    @Query("SELECT * FROM saved_games WHERE id = :gameId")
    suspend fun getGameWithMoves(gameId: Long): SavedGameWithMoves?

    @Insert
    suspend fun insertGame(game: SavedGame): Long

    @Update
    suspend fun updateGame(game: SavedGame)

    @Delete
    suspend fun deleteGame(game: SavedGame)

    @Query("DELETE FROM saved_games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: Long)

    @Query("DELETE FROM saved_games WHERE updatedAt < :timestamp")
    suspend fun deleteGamesOlderThan(timestamp: Long)
}