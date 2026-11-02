package com.nihad64.rotam.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.prefs.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())
    val hasBuildKey = com.nihad64.rotam.BuildConfig.GEMINI_API_KEY.isNotBlank()

    fun canScheduleExact() = c.scheduler.canScheduleExact()

    fun setName(v: String) = viewModelScope.launch { c.settings.setName(v) }
    fun setReminders(v: Boolean) = viewModelScope.launch { c.settings.setReminders(v); c.scheduler.rescheduleAll() }
    fun setReminderMinutes(v: Int) = viewModelScope.launch { c.settings.setReminderMinutes(v); c.scheduler.rescheduleAll() }
    fun setQuiet(v: Boolean) = viewModelScope.launch { c.settings.setQuiet(v) }
    fun setSounds(v: Boolean) = viewModelScope.launch { c.settings.setSounds(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { c.settings.setHaptics(v) }
    fun setTheme(v: String) = viewModelScope.launch { c.settings.setTheme(v) }
    fun setGeminiKey(v: String) = viewModelScope.launch { c.settings.setGeminiKey(v) }
    fun resetAll() = viewModelScope.launch { c.planRepo.clear(); c.goalRepo.clear() }
}
