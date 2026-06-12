package com.example.phonething.ui.fragments

import org.json.JSONArray
import org.json.JSONObject

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startDay: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("startDay", startDay)
    }

    companion object {
        fun fromJson(obj: JSONObject): CalendarEvent =
            CalendarEvent(
                id = obj.getLong("id"),
                title = obj.getString("title"),
                startDay = obj.getInt("startDay")
            )
    }
}

fun List<CalendarEvent>.toJsonArray(): JSONArray =
    JSONArray().apply { forEach { put(it.toJson()) } }

fun JSONArray.toCalendarEventList(): List<CalendarEvent> =
    (0 until length()).map { CalendarEvent.fromJson(getJSONObject(it)) }
