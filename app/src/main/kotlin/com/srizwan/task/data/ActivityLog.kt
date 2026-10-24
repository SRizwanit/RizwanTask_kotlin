package com.srizwan.task.data

import android.content.Context
import com.srizwan.task.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * কার্যকলাপের লগ (common.js-এর log)। ব্যর্থ হলেও অ্যাপ থামে না;
 * activity_log টেবিল না থাকলে নিঃশব্দে বাদ যায়।
 */
object ActivityLog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(context: Context, action: String, detail: String = "") {
        val app = context.applicationContext
        val s = Session.get(app)
        scope.launch {
            try {
                val row = JSONObject().apply {
                    if (s != null && s.has("id")) put("user_id", s.optInt("id").toString())
                    put("user_name", s?.optString("name", "") ?: "")
                    put("action", action)
                    put("detail", detail.take(500))
                }
                Api(app).postMin("activity_log", row)
            } catch (e: Exception) {
                // লগ ব্যর্থ হলে উপেক্ষা
            }
        }
    }
}
