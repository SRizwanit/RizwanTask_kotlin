package com.srizwan.task.data

import android.content.Context
import com.srizwan.task.Session
import org.json.JSONObject

/** common.js-এর requireAuth-এর ফলাফল */
sealed class AuthResult {
    /** তাজা ইউজার সারি */
    data class Ok(val user: JSONObject) : AuthResult()
    /** সেশন নেই / ইউজার নেই / ডিভাইস মেলেনি → লগইনে পাঠাও (সেশন মুছে) */
    object Logout : AuthResult()
    /** ব্লক করা ইউজার (অ্যাডমিন বাদে) */
    data class Blocked(val user: JSONObject) : AuthResult()
    /** অ্যাডমিন-পেজে সাধারণ ইউজার → টাস্কে পাঠাও */
    object NotAdmin : AuthResult()
    data class NetworkError(val message: String) : AuthResult()
}

/** common.js-এর requireAuth / touchActive */
object Auth {
    private const val PREFS = "kp_touch"
    private const val KEY = "kp_touch_v1"

    suspend fun requireAuth(context: Context, admin: Boolean = false): AuthResult {
        val app = context.applicationContext
        val s = Session.get(app) ?: return AuthResult.Logout
        val row: JSONObject? = try {
            val r = Api(app).get("app_users?id=eq.${enc(s.optInt("id").toString())}&limit=1")
            if (r.length() > 0) r.getJSONObject(0) else null
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            return AuthResult.NetworkError(e.message ?: "")
        }
        val u = row ?: return logout(app)
        val dev = u.optString("device_id", "")
        if (Session.deviceEmpty(if (u.isNull("device_id")) null else dev) || dev != Session.deviceId(app)) return logout(app)
        val isAdmin = Phone.isAdminPhone(u.optString("phone"))
        if (u.optBoolean("is_blocked", false) && !isAdmin) return AuthResult.Blocked(u)
        if (admin && !isAdmin) return AuthResult.NotAdmin
        touchActive(app, u)
        return AuthResult.Ok(u)
    }

    private fun logout(app: Context): AuthResult {
        Session.clear(app)
        return AuthResult.Logout
    }

    /** সর্বশেষ সক্রিয় সময় (৫ মিনিটে একবার; কলাম না থাকলে নীরবে বাদ) */
    private suspend fun touchActive(app: Context, u: JSONObject) {
        val now = System.currentTimeMillis()
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            if (now - p.getLong(KEY, 0L) < 300_000L) return
            p.edit().putLong(KEY, now).apply()
        } catch (e: Exception) {
            // উপেক্ষা
        }
        try {
            val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(now))
            Api(app).patchMin("app_users?id=eq.${enc(u.opt("id").toString())}", JSONObject().put("last_active_at", iso))
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            // উপেক্ষা
        }
    }
}
