package com.akshat.hydrify

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import org.json.JSONObject

object DataStoreManager {
    private const val DATASTORE_NAME = "hydrify_prefs"
    private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

    private val ONBOARDED = booleanPreferencesKey("onboarded")
    private val DAILY_GOAL = intPreferencesKey("daily_goal")
    private val TODAY_INTAKE = intPreferencesKey("today_intake")
    private val LAST_LOG_DATE = intPreferencesKey("last_log_date") // yyyymmdd
    private val INTAKE_HISTORY = stringPreferencesKey("intake_history")

    suspend fun setOnboarded(context: Context, onboarded: Boolean) {
        context.dataStore.edit { it[ONBOARDED] = onboarded }
    }
    fun isOnboarded(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDED] ?: false }

    suspend fun setDailyGoal(context: Context, goal: Int) {
        context.dataStore.edit { it[DAILY_GOAL] = goal }
    }
    fun getDailyGoal(context: Context): Flow<Int> =
        context.dataStore.data.map { it[DAILY_GOAL] ?: 2000 }

    suspend fun addWaterIntake(context: Context, amount: Int) {
        val today = LocalDate.now().toString().replace("-", "").toInt()
        context.dataStore.edit {
            val lastDate = it[LAST_LOG_DATE] ?: today
            val current = if (lastDate == today) it[TODAY_INTAKE] ?: 0 else 0
            it[TODAY_INTAKE] = current + amount
            it[LAST_LOG_DATE] = today
            // Update intake history
            val historyJson = it[INTAKE_HISTORY] ?: "{}"
            val historyObj = JSONObject(historyJson)
            val todayStr = today.toString()
            val prev = historyObj.optInt(todayStr, 0)
            historyObj.put(todayStr, prev + amount)
            // Keep only last 7 days
            val keys = historyObj.keys().asSequence().toList().sortedDescending()
            for (k in keys.drop(7)) historyObj.remove(k)
            it[INTAKE_HISTORY] = historyObj.toString()
        }
    }
    fun getTodayIntake(context: Context): Flow<Int> {
        val today = LocalDate.now().toString().replace("-", "").toInt()
        return context.dataStore.data.map {
            val lastDate = it[LAST_LOG_DATE] ?: today
            if (lastDate == today) it[TODAY_INTAKE] ?: 0 else 0
        }
    }
    suspend fun resetTodayIntake(context: Context) {
        context.dataStore.edit { it[TODAY_INTAKE] = 0 }
    }
    fun getLast7DaysIntake(context: Context): Flow<List<Pair<String, Int>>> {
        return context.dataStore.data.map {
            val historyJson = it[INTAKE_HISTORY] ?: "{}"
            val historyObj = JSONObject(historyJson)
            val today = LocalDate.now()
            (0..6).map { i ->
                val date = today.minusDays((6 - i).toLong())
                val key = date.toString().replace("-", "")
                val label = date.dayOfWeek.name.take(3)
                label to historyObj.optInt(key, 0)
            }
        }
    }
} 