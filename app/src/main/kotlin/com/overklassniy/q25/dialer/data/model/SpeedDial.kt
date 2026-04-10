package com.overklassniy.q25.dialer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dials")
data class SpeedDial(
    @PrimaryKey
    val slot: Int,
    val number: String,
    val name: String,
)