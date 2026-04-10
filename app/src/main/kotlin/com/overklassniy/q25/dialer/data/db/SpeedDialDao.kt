package com.overklassniy.q25.dialer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.overklassniy.q25.dialer.data.model.SpeedDial
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDialDao {

    @Query("SELECT * FROM speed_dials ORDER BY slot ASC")
    fun getAll(): Flow<List<SpeedDial>>

    @Query("SELECT * FROM speed_dials WHERE slot = :slot")
    suspend fun getBySlot(slot: Int): SpeedDial?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(speedDial: SpeedDial)

    @Query("DELETE FROM speed_dials WHERE slot = :slot")
    suspend fun deleteBySlot(slot: Int)
}