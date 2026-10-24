package com.srizwan.task.data

import com.srizwan.task.BuildConfig

/**
 * Phone validation (common.js Phone object-এর পোর্ট)
 */
object Phone {
    data class Result(val ok: Boolean, val phone: String, val msg: String, val foreign: Boolean)

    fun isBD(raw: String): Boolean {
        val p = raw.trim().replace(Regex("[\\s-]"), "")
        return p.startsWith("01") || p.startsWith("+8801") || p.startsWith("8801") || p.startsWith("+880")
    }

    fun normBD(raw: String): String {
        var p = raw.trim().replace(Regex("[\\s-]"), "")
        if (p.startsWith("+880")) p = "0" + p.substring(4)
        else if (p.startsWith("880")) p = "0" + p.substring(3)
        return p
    }

    fun validate(raw: String): Result {
        val t = raw.trim()
        if (t.isEmpty()) return Result(false, "", "ফোন নাম্বার দিন", false)

        val c = t.replace(Regex("[\\s-]"), "")
        if (isBD(t)) {
            val n = normBD(t)
            if (n.length != 11) return Result(
                false, n,
                "বাংলাদেশি নাম্বার ১১ ডিজিটের হতে হবে (যেমন: 017XXXXXXXX)", false
            )
            if (!Regex("^01[0-9]{9}$").matches(n)) return Result(
                false, n, "সঠিক বাংলাদেশি নাম্বার দিন", false
            )
            return Result(true, n, "OK", false)
        }

        val f = c.replace("+", "")
        if (!Regex("^[0-9]{7,15}$").matches(f)) return Result(
            false, t, "সঠিক বিদেশি নাম্বার দিন (৭-১৫ ডিজিট, + সহ)", true
        )
        return Result(true, if (c.startsWith("+")) c else "+$f", "OK", true)
    }

    fun isAdminPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        return normBD(phone) == BuildConfig.ADMIN_PHONE
    }
}
