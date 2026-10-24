package com.srizwan.task.task

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/* helpers.js: লক্ষ্য/স্ট্রিক/ব্যাজ, "চালিয়ে যান", ডিভাইসে সংরক্ষিত মান (localStorage → SharedPreferences) */

private const val PREFS = "kp_task_v1"

fun resumeKey(id: String) = "kp_resume_v1_$id"
fun goalKey(id: String) = "kp_goal_v1_$id"
fun goalHitKey(id: String, day: LocalDate) = "kp_goalhit_v1_${id}_${day.year}-${day.monthValue}-${day.dayOfMonth}"

private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

/** দৈনিক লক্ষ্য; না থাকলে ২০ */
fun readGoal(context: Context, id: String): Int {
    return try {
        val g = prefs(context).getString(goalKey(id), null)?.toDoubleOrNull()?.toInt() ?: 0
        if (g > 0) g else 20
    } catch (e: Exception) {
        20
    }
}

fun writeGoal(context: Context, id: String, g: Int) {
    try {
        prefs(context).edit().putString(goalKey(id), g.toString()).apply()
    } catch (e: Exception) {
        // উপেক্ষা
    }
}

/** View স্ক্রিন (ধাপ ৭) প্রতিবার পেইজ বদলালে `kp_resume_v1_<id>`-এ {"b":..,"s":..} লিখবে */
fun getResume(context: Context, id: String): Resume? {
    return try {
        val raw = prefs(context).getString(resumeKey(id), null) ?: return null
        val o = JSONObject(raw)
        if (o.isNull("b")) null else Resume(o.opt("b").toString(), o.str("s") ?: "")
    } catch (e: Exception) {
        null
    }
}

fun writeResume(context: Context, id: String, bookId: String, stem: String) {
    try {
        val o = JSONObject().put("b", bookId).put("s", stem)
        prefs(context).edit().putString(resumeKey(id), o.toString()).apply()
    } catch (e: Exception) {
        // উপেক্ষা
    }
}

/** আজ লক্ষ্য-পূরণের বার্তা দেখানো হয়েছে কি */
fun goalHitShown(context: Context, id: String): Boolean = try {
    prefs(context).getString(goalHitKey(id, LocalDate.now()), null) != null
} catch (e: Exception) {
    false
}

fun markGoalHitShown(context: Context, id: String) {
    try {
        prefs(context).edit().putString(goalHitKey(id, LocalDate.now()), "1").apply()
    } catch (e: Exception) {
        // উপেক্ষা
    }
}

/* ---- টানা দিন (স্ট্রিক) ---- */

fun streakInfo(proofs: List<Proof>): StreakInfo {
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)
    val t0 = now.atStartOfDay(zone).toInstant().toEpochMilli()
    val days = HashSet<LocalDate>()
    var today = 0
    for (p in proofs) {
        val t = p.createdAt ?: continue
        if (t <= 0L) continue
        days.add(Instant.ofEpochMilli(t).atZone(zone).toLocalDate())
        if (t >= t0) today++
    }
    val doneToday = days.contains(now)
    var cur = 0
    var d = if (doneToday) now else now.minusDays(1)
    while (days.contains(d)) {
        cur++
        d = d.minusDays(1)
    }
    val nums = days.map { it.toEpochDay() }.sorted()
    var best = 0
    var run = 0
    var prev = Long.MIN_VALUE
    for (n in nums) {
        run = if (prev != Long.MIN_VALUE && n == prev + 1) run + 1 else 1
        prev = n
        if (run > best) best = run
    }
    return StreakInfo(today, cur, best, doneToday)
}

/** goalToast-এর শর্ত: লক্ষ্য পূরণ হলে (এবং created_at থাকলে) আজকের স্ট্রিক তথ্য */
fun goalReached(proofs: List<Proof>, goal: Int): StreakInfo? {
    if (proofs.isEmpty() || !proofs[0].hasCreatedAt) return null
    val s = streakInfo(proofs)
    return if (s.today >= goal) s else null
}

/** "চালিয়ে যান": এই ডিভাইসে রেকর্ড থাকলে সেই কিতাব/পেইজ; না থাকলে সর্বশেষ প্রুফড কিতাব */
fun resumeTarget(resume: Resume?, booksById: Map<String, Book>, proofs: List<Proof>): ResumeTarget? {
    if (resume != null) {
        val b = booksById[resume.b]
        if (b != null) return ResumeTarget(b, resume.b, resume.s)
    }
    if (proofs.isNotEmpty() && proofs[0].createdAt != null) {
        val first = proofs[0]
        val b = booksById[first.bookId]
        if (b != null) return ResumeTarget(b, first.bookId, "")
    }
    return null
}

/* ---- নোটিশ "দেখা হয়েছে" ও প্রথমবারের গাইড (helpers.js: readSeen/writeSeen/guideKey) ---- */

fun noticeKey(id: String) = "kp_notice_seen_$id"
fun guideKey(id: String) = "kp_guide_v2_$id"

fun readSeen(context: Context, id: String): Set<String> {
    return try {
        val raw = prefs(context).getString(noticeKey(id), null) ?: return emptySet()
        val a = org.json.JSONArray(raw)
        (0 until a.length()).map { a.get(it).toString() }.toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

/** সর্বশেষ ৩০০টি রাখা হয় */
fun writeSeen(context: Context, id: String, seen: Set<String>) {
    try {
        val a = org.json.JSONArray()
        seen.toList().takeLast(300).forEach { a.put(it) }
        prefs(context).edit().putString(noticeKey(id), a.toString()).apply()
    } catch (e: Exception) {
        // উপেক্ষা
    }
}

fun guideSeen(context: Context, id: String): Boolean = try {
    prefs(context).getString(guideKey(id), null) == "1"
} catch (e: Exception) {
    false
}

fun markGuideSeen(context: Context, id: String) {
    try {
        prefs(context).edit().putString(guideKey(id), "1").apply()
    } catch (e: Exception) {
        // উপেক্ষা
    }
}

/**
 * React-এর `task#guide` হ্যাশ: প্রোফাইলের "ব্যবহারের গাইড" বোতাম টাস্কে গিয়ে গাইড খুলত।
 * প্রোফাইল স্ক্রিন (ধাপ ৫) নেভিগেট করার আগে `GuideRequest.pending = true` দেবে।
 */
object GuideRequest {
    @Volatile
    var pending: Boolean = false

    /** প্রোফাইলের নিচের বার (React: `task#pay` ইত্যাদি): "task" | "pay" | "rank" */
    @Volatile
    var tab: String? = null
}
