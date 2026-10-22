package com.srizwan.task

import android.content.Context
import com.srizwan.task.data.AppUser
import org.json.JSONObject
import java.util.UUID

data class SessionData(val id: String, val phone: String?, val name: String?)

object Session {
    private const val PREFS = "kp_session"
    private const val KEY = "kp_session_v1"
    private const val DEVICE_KEY = "kp_device_v1"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun exists(context: Context): Boolean = prefs(context).getString(KEY, null) != null

    fun get(context: Context): SessionData? = runCatching {
        val o = JSONObject(prefs(context).getString(KEY, null) ?: return null)
        SessionData(o.get("id").toString(), o.optString("phone", null), o.optString("name", null))
    }.getOrNull()

    fun set(context: Context, u: AppUser) {
        val o = JSONObject().put("id", u.id).put("phone", u.phone ?: JSONObject.NULL).put("name", u.fullName ?: JSONObject.NULL)
        prefs(context).edit().putString(KEY, o.toString()).apply()
    }

    fun clear(context: Context) = prefs(context).edit().remove(KEY).apply()

    /** এই ডিভাইসের স্থায়ী আইডি (React-এর localStorage kp_device_v1-এর মতো)। */
    fun deviceId(context: Context): String {
        val p = prefs(context)
        p.getString(DEVICE_KEY, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val d = UUID.randomUUID().toString()
        p.edit().putString(DEVICE_KEY, d).apply()
        return d
    }
}
