package com.pocketbook.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromTagIdsList(value: List<String>?): String? {
        if (value == null) return null
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toTagIdsList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        val jsonArray = JSONArray(value)
        val result = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }
}
