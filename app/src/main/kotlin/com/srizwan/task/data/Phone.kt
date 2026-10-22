package com.srizwan.task.data

import com.srizwan.task.BuildConfig

data class PhoneResult(val ok: Boolean, val phone: String, val msg: String, val foreign: Boolean)

/** ফোন ভ্যালিডেশন (React Phone অবজেক্টের হুবহু রূপ)। */
object Phone {
    private fun clean(raw: String) = raw.trim().replace(Regex("[\\s-]"), "")

    fun isBD(raw: String): Boolean {
        val p = clean(raw)
        return p.startsWith("01") || p.startsWith("+8801") || p.startsWith("8801") || p.startsWith("+880")
    }

    fun normBD(raw: String): String {
        var p = clean(raw)
        if (p.startsWith("+880")) p = "0" + p.substring(4)
        else if (p.startsWith("880")) p = "0" + p.substring(3)
        return p
    }

    fun validate(raw: String?): PhoneResult {
        val t = (raw ?: "").trim()
        if (t.isEmpty()) return PhoneResult(false, "", "ফোন নাম্বার দিন", false)
        val c = clean(t)
        if (isBD(t)) {
            val n = normBD(t)
            if (n.length != 11) return PhoneResult(false, n, "বাংলাদেশি নাম্বার ১১ ডিজিটের হতে হবে (যেমন: 017XXXXXXXX)", false)
            if (!Regex("^01[0-9]{9}$").matches(n)) return PhoneResult(false, n, "সঠিক বাংলাদেশি নাম্বার দিন", false)
            return PhoneResult(true, n, "OK", false)
        }
        val f = c.replaceFirst("+", "")
        if (!Regex("^[0-9]{7,15}$").matches(f)) return PhoneResult(false, t, "সঠিক বিদেশি নাম্বার দিন (৭-১৫ ডিজিট, + সহ)", true)
        return PhoneResult(true, if (c.startsWith("+")) c else "+$f", "OK", true)
    }

    fun isAdminPhone(p: String?): Boolean = !p.isNullOrEmpty() && normBD(p) == BuildConfig.ADMIN_PHONE
}
