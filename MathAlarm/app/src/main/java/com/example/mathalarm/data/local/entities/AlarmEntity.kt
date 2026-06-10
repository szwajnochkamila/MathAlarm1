package com.example.mathalarm.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isActive: Boolean = true // czy budzik jest wlaczony, czy wylaczony
)