package com.example.mathalarm.repository.impl

import com.example.mathalarm.data.local.dao.AlarmDao
import com.example.mathalarm.data.local.entities.AlarmEntity
import com.example.mathalarm.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalAlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<AlarmEntity>> {
        return alarmDao.getAllAlarms()
    }

    override suspend fun insertAlarm(alarm: AlarmEntity): Long {
        return alarmDao.insert(alarm)
    }

    override suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.update(alarm)
    }

    override suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarmDao.delete(alarm)
    }

    override suspend fun getAlarmByTime(hour: Int, minute: Int): AlarmEntity? {
        return alarmDao.getAlarmByTime(hour, minute)
    }
}