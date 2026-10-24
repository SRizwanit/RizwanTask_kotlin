package com.srizwan.task

import android.content.Context
import org.json.JSONObject
import java.util.UUID

object Session {
    private const val PREFS = "kp_session"
    private const val KEY = "kp_session_v1"
    private const val DEVICE_KEY = "kp_device_v1"

    fun exists(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) != null

    fun get(context: Context): JSONObject? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return if (json != null) try {
            JSONObject(json)
        } catch (e: Exception) {
            null
        } else null
    }

    fun set(context: Context, user: JSONObject) {
        val session = JSONObject().apply {
            put("id", user.optInt("id"))
            put("phone", user.optString("phone"))
            put("name", user.optString("full_name"))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, session.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(DEVICE_KEY, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(DEVICE_KEY, id).apply()
        }
        return id
    }

    fun deviceEmpty(device: String?): Boolean {
        return device.isNullOrBlank() || device.equals("null", ignoreCase = true)
    }
}
