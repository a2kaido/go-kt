package com.github.a2kaido.go.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.a2kaido.go.android.data.converter.DateConverter
import com.github.a2kaido.go.android.data.dao.MoveRecordDao
import com.github.a2kaido.go.android.data.dao.SavedGameDao
import com.github.a2kaido.go.android.data.entity.MoveRecord
import com.github.a2kaido.go.android.data.entity.SavedGame

@Database(
    entities = [SavedGame::class, MoveRecord::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class GoDatabase : RoomDatabase() {
    abstract fun savedGameDao(): SavedGameDao
    abstract fun moveRecordDao(): MoveRecordDao

    companion object {
        @Volatile
        private var INSTANCE: GoDatabase? = null

        fun getDatabase(context: Context): GoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoDatabase::class.java,
                    "go_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}