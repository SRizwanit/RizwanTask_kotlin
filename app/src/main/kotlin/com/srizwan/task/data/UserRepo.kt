package com.srizwan.task.data

import org.json.JSONObject
import java.time.Instant

data class AppUser(
    val id: String,
    val phone: String?,
    val fullName: String?,
    var deviceId: String?,
    val passwordHash: String?,
    val passwordSalt: String?,
    val isBlocked: Boolean,
    val blockedReason: String?
) {
    companion object {
        private fun JSONObject.str(k: String): String? = if (isNull(k)) null else optString(k)
        fun from(o: JSONObject) = AppUser(
            id = o.get("id").toString(),
            phone = o.str("phone"),
            fullName = o.str("full_name"),
            deviceId = o.str("device_id"),
            passwordHash = o.str("password_hash"),
            passwordSalt = o.str("password_salt"),
            isBlocked = o.optBoolean("is_blocked", false),
            blockedReason = o.str("blocked_reason")
        )
    }
}

fun deviceEmpty(d: String?): Boolean = d == null || d.trim().isEmpty() || d.trim().lowercase() == "null"

object UserRepo {
    private val enc = Api::enc

    suspend fun findUserByPhone(phone: String): AppUser? {
        val n = Phone.normBD(phone)
        var r = Api.get("app_users?phone=eq.${enc(phone)}&limit=1")
        if (r.length() == 0 && n != phone) r = Api.get("app_users?phone=eq.${enc(n)}&limit=1")
        return if (r.length() > 0) AppUser.from(r.getJSONObject(0)) else null
    }

    suspend fun findUserById(id: String): AppUser? {
        val r = Api.get("app_users?id=eq.${enc(id)}&limit=1")
        return if (r.length() > 0) AppUser.from(r.getJSONObject(0)) else null
    }

    suspend fun findUserByDevice(dev: String, excludeId: String? = null): AppUser? {
        var p = "app_users?device_id=eq.${enc(dev)}&limit=1"
        if (excludeId != null) p += "&id=neq.${enc(excludeId)}"
        val r = Api.get(p)
        return if (r.length() > 0) AppUser.from(r.getJSONObject(0)) else null
    }

    suspend fun patchUser(id: String, fields: JSONObject) {
        Api.patch("app_users?id=eq.${enc(id)}", fields)
    }

    /** OTP পুল (otp_codes টেবিল) থেকে একটা কোড নিয়ে ফোনে বরাদ্দ করে। */
    suspend fun requestOtp(phone: String): String {
        val a = Api.get("otp_codes?status=eq.available&limit=1&order=id.asc")
        if (a.length() == 0) throw Exception("OTP পুল খালি, অ্যাডমিনকে জানান")
        val row = a.getJSONObject(0)
        val now = Instant.now()
        Api.patch(
            "otp_codes?id=eq.${row.get("id")}",
            JSONObject()
                .put("status", "assigned")
                .put("phone", phone)
                .put("assigned_at", now.toString())
                .put("expires_at", now.plusSeconds(120).toString())
        )
        return row.get("code").toString()
    }

    suspend fun verifyOtp(phone: String, code: String): Boolean {
        val r = Api.get("otp_codes?phone=eq.${enc(phone)}&code=eq.${enc(code)}&status=eq.assigned&order=id.desc&limit=1")
        if (r.length() == 0) return false
        val o = r.getJSONObject(0)
        val exp = if (o.isNull("expires_at")) null else o.optString("expires_at")
        if (!exp.isNullOrEmpty() && runCatching { java.time.OffsetDateTime.parse(exp).toInstant() }
                .getOrNull()?.isBefore(Instant.now()) == true) {
            Api.patch("otp_codes?id=eq.${o.get("id")}", JSONObject().put("status", "expired"))
            return false
        }
        Api.patch("otp_codes?id=eq.${o.get("id")}", JSONObject().put("status", "verified"))
        return true
    }
}
