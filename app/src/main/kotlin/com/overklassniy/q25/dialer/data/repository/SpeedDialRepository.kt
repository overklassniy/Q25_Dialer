package com.overklassniy.q25.dialer.data.repository

import com.overklassniy.q25.dialer.data.db.AppDatabase
import com.overklassniy.q25.dialer.data.model.SpeedDial
import kotlinx.coroutines.flow.Flow

class SpeedDialRepository(database: AppDatabase) {

    private val dao = database.speedDialDao()

    fun getAll(): Flow<List<SpeedDial>> = dao.getAll()

    suspend fun getBySlot(slot: Int): SpeedDial? = dao.getBySlot(slot)

    suspend fun set(speedDial: SpeedDial) = dao.insert(speedDial)

    suspend fun remove(slot: Int) = dao.deleteBySlot(slot)
}