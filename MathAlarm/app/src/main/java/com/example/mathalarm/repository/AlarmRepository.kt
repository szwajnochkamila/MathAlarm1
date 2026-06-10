package com.example.mathalarm.repository

import com.example.mathalarm.data.local.entities.AlarmEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<AlarmEntity>>
    suspend fun insertAlarm(alarm: AlarmEntity): Long
    suspend fun updateAlarm(alarm: AlarmEntity)
    suspend fun deleteAlarm(alarm: AlarmEntity)
    suspend fun getAlarmByTime(hour: Int, minute: Int): AlarmEntity?
}