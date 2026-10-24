package com.srizwan.task.data

import android.content.Context
import org.json.JSONObject
import java.net.URLEncoder

/**
 * User CRUD (common.js-এর findUserByPhone, requestOtp, verifyOtp ইত্যাদি)
 */
class UserRepo(private val api: Api) {
    suspend fun findByPhone(phone: String): JSONObject? {
        val n = Phone.normBD(phone)
        var arr = api.get("app_users?phone=eq.${enc(phone)}&limit=1")
        if (arr.length() == 0 && n != phone) {
            arr = api.get("app_users?phone=eq.${enc(n)}&limit=1")
        }
        return if (arr.length() > 0) arr.getJSONObject(0) else null
    }

    suspend fun findByDevice(deviceId: String, excludeId: Int? = null): JSONObject? {
        var path = "app_users?device_id=eq.${enc(deviceId)}&limit=1"
        if (excludeId != null) path += "&id=neq.$excludeId"
        val arr = api.get(path)
        return if (arr.length() > 0) arr.getJSONObject(0) else null
    }

    suspend fun requestOtp(phone: String): String {
        val arr = api.get("otp_codes?status=eq.available&limit=1&order=id.asc")
        if (arr.length() == 0) throw ApiException("OTP পুল খালি, অ্যাডমিনকে জানান")
        val row = arr.getJSONObject(0)
        val code = row.getString("code")
        val exp = System.currentTimeMillis() + 2 * 60000
        val body = JSONObject().apply {
            put("status", "assigned")
            put("phone", phone)
            put("assigned_at", iso(System.currentTimeMillis()))
            put("expires_at", iso(exp))
        }
        api.patch("otp_codes?id=eq.${row.getInt("id")}", body)
        return code
    }

    suspend fun verifyOtp(phone: String, code: String): Boolean {
        val arr = api.get("otp_codes?phone=eq.${enc(phone)}&code=eq.${enc(code)}&status=eq.assigned&order=id.desc&limit=1")
        if (arr.length() == 0) return false
        val o = arr.getJSONObject(0)
        val exp = o.optString("expires_at", "")
        if (exp.isNotEmpty() && parseIso(exp) < System.currentTimeMillis()) {
            api.patch("otp_codes?id=eq.${o.getInt("id")}", JSONObject().put("status", "expired"))
            return false
        }
        api.patch("otp_codes?id=eq.${o.getInt("id")}", JSONObject().put("status", "verified"))
        return true
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun iso(ms: Long) = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date(ms))

    private fun parseIso(s: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(s)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}
