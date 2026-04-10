package com.overklassniy.q25.dialer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.overklassniy.q25.dialer.data.model.SpeedDial

@Database(
    entities = [SpeedDial::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun speedDialDao(): SpeedDialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "q25_dialer.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}