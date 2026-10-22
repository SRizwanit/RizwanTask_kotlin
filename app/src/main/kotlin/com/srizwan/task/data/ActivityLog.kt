package com.srizwan.task.data

import org.json.JSONObject

/** কার্যকলাপের লগ — ব্যর্থ হলেও অ্যাপ থামে না। */
object ActivityLog {
    suspend fun log(action: String, detail: String = "", userId: String?, userName: String?) {
        runCatching {
            Api.postMin(
                "activity_log",
                JSONObject()
                    .put("user_id", userId ?: JSONObject.NULL)
                    .put("user_name", userName ?: "")
                    .put("action", action)
                    .put("detail", detail.take(500))
            )
        }
    }
}
