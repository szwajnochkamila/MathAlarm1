package com.example.mathalarm.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mathalarm.data.local.entities.AlarmEntity
import com.example.mathalarm.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    // wstrzykujemy obiekt AlarmRepository do ViewModel
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    val alarmsState: StateFlow<List<AlarmEntity>> = alarmRepository.getAllAlarms()
        .stateIn(
            scope = viewModelScope, // zapytanie do bazy zniknie i przestanie zuzywac pamiec
                                    // gdy ViewModel zostanie ostatecznie zniszczony np. uzytkownik wylaczy aplikacje
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAlarm(hour: Int, minute: Int, onSuccess: () -> Unit, onDuplicate: () -> Unit) {
        viewModelScope.launch {
            // 1. Sprawdzenie, czy w bazie jest juz budzik na te godzine
            val existingAlarm = alarmRepository.getAlarmByTime(hour, minute)

            if (existingAlarm != null) {
                // 2. Jesli istnieje - nie zapisujemy i informujemy widok o bledzie
                onDuplicate()
            } else {
                // 3. Jesli nie ma - dodajemy do bazy i informujemy o sukcesie
                alarmRepository.insertAlarm(AlarmEntity(hour = hour, minute = minute))
                onSuccess()
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarm)
        }
    }

    fun toggleAlarmActive(alarm: AlarmEntity, isActive: Boolean) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm.copy(isActive = isActive))
        }
    }
}