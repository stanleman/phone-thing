package com.example.phonething.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "phone_thing_data"
private const val KEY_CALENDAR_EVENTS = "calendar_events"
private const val KEY_TODO_TASKS = "todo_tasks"
private const val KEY_HAS_SEEDED = "has_seeded_sample_data"

object StorageUtil {

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Calendar events ─────────────────────────────────────────

    fun loadEvents(ctx: Context): MutableList<CalendarEvent> {
        val json = prefs(ctx).getString(KEY_CALENDAR_EVENTS, null) ?: return mutableListOf()
        return try {
            JSONArray(json).toCalendarEventList().toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveEvents(ctx: Context, events: List<CalendarEvent>) {
        prefs(ctx).edit().putString(KEY_CALENDAR_EVENTS, events.toJsonArray().toString()).apply()
    }

    // ── Todo tasks ──────────────────────────────────────────────

    fun loadTasks(ctx: Context): MutableList<TaskItem> {
        val json = prefs(ctx).getString(KEY_TODO_TASKS, null) ?: return mutableListOf()
        return try {
            JSONArray(json).toTaskItemList().toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveTasks(ctx: Context, tasks: List<TaskItem>) {
        prefs(ctx).edit().putString(KEY_TODO_TASKS, tasks.toJsonArray().toString()).apply()
    }

    // ── First-launch seeding flag ───────────────────────────────

    fun hasSeeded(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_HAS_SEEDED, false)

    fun markSeeded(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_HAS_SEEDED, true).apply()
    }
}
