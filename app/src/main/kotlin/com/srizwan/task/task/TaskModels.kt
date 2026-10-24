package com.srizwan.task.task

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/* ---------- JSON সহায়ক: JSON null ও অনুপস্থিত কী দুটোই null ---------- */

fun JSONObject.str(key: String): String? {
    if (isNull(key)) return null
    return opt(key)?.toString()
}

fun JSONObject.dbl(key: String): Double? {
    if (isNull(key)) return null
    return when (val v = opt(key)) {
        is Number -> v.toDouble()
        is String -> v.trim().toDoubleOrNull()
        else -> null
    }
}

/** Supabase timestamptz → epoch millis; পার্স না হলে null */
fun parseTime(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(s).toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            Instant.parse(s).toEpochMilli()
        } catch (e2: Exception) {
            try {
                LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (e3: Exception) {
                null
            }
        }
    }
}

/* ---------- মডেল ---------- */

/** `books` টেবিলের সারি */
data class Book(
    val id: String,
    val name: String,
    val author: String,
    val coverUrl: String?,
    val totalPages: Int,
    val pageRate: Double?
)

fun parseBook(o: JSONObject): Book = Book(
    id = o.str("id") ?: "",
    name = o.str("name") ?: "",
    author = o.str("author") ?: "",
    coverUrl = o.str("cover_url"),
    totalPages = (o.dbl("total_pages") ?: 0.0).toInt(),
    pageRate = o.dbl("page_rate")
)

/** `page_proofs` টেবিলের সারি (নিজের প্রুফড পেইজ) */
data class Proof(
    val bookId: String,
    val stem: String,
    val reviewStatus: String,
    val reviewNote: String?,
    val rate: Double?,
    val createdAt: Long?,
    /** সংকীর্ণ কোয়েরিতে created_at কলামই আসে না (React-এ undefined) */
    val hasCreatedAt: Boolean
)

fun parseProof(o: JSONObject): Proof = Proof(
    bookId = o.str("book_id") ?: "",
    stem = o.str("stem") ?: "",
    reviewStatus = o.str("review_status")?.ifEmpty { null } ?: "pending",
    reviewNote = o.str("review_note"),
    rate = o.dbl("rate"),
    createdAt = parseTime(o.str("created_at")),
    hasCreatedAt = o.has("created_at")
)

/** `payments` টেবিলের সারি */
data class Pay(
    val id: String?,
    val amount: Double,
    val status: String,
    val method: String?,
    val number: String?,
    val pages: Int?,
    val note: String?,
    val createdAt: String?,
    val processedAt: String?
)

fun parsePay(o: JSONObject): Pay = Pay(
    id = o.str("id"),
    amount = o.dbl("amount") ?: 0.0,
    status = o.str("status") ?: "",
    method = o.str("method"),
    number = o.str("number"),
    pages = o.dbl("pages")?.toInt(),
    note = o.str("note"),
    createdAt = o.str("created_at"),
    processedAt = o.str("processed_at")
)

/** `app_settings` (id=1): page_rate, min_payout, warn_pct, warn_min */
data class AppSettings(
    val pageRate: Double? = null,
    val minPayout: Double? = null,
    val warnPct: Double? = null,
    val warnMin: Double? = null
)

fun parseSettings(o: JSONObject): AppSettings = AppSettings(
    pageRate = o.dbl("page_rate"),
    minPayout = o.dbl("min_payout"),
    warnPct = o.dbl("warn_pct"),
    warnMin = o.dbl("warn_min")
)

/** লগইন করা ইউজার (app_users থেকে) */
data class MeInfo(
    val id: String,
    val phone: String,
    val fullName: String,
    val paymentMethod: String?,
    val paymentNumber: String?,
    /** নোটিশ ফিল্টারে লাগে: নতুন ইউজার আগের নোটিশ দেখবে না */
    val createdAt: String? = null
)

/** সেটিংস + আমার প্রুফড পেইজ + পেমেন্ট */
data class Fin(
    val s: AppSettings = AppSettings(),
    val proofs: List<Proof> = emptyList(),
    val pays: List<Pay> = emptyList(),
    val ready: Boolean = false
)

data class BookSum(
    val n: Int = 0,
    val approved: Int = 0,
    val pending: Int = 0,
    val correction: Int = 0,
    val earned: Double = 0.0
)

/** App.summarize-এর ফলাফল */
data class Summary(
    val pages: Int,
    val approved: Int,
    val pending: Int,
    val correction: Int,
    val earned: Double,
    val pendingAmt: Double,
    val paid: Double,
    val requested: Double,
    val available: Double,
    val byBook: Map<String, BookSum>
)

enum class BooksStatus { LOADING, READY, ERROR }

/* ---------- র‍্যাংক ---------- */

enum class RankPeriod(val key: String, val label: String, val chip: String) {
    DAY("day", "আজ", "আজ"),
    WEEK("week", "এই সপ্তাহে", "এই সপ্তাহ"),
    ALL("all", "সব মিলিয়ে", "সব সময়")
}

data class RankRow(val id: String, val n: Long)

data class RankData(
    val period: RankPeriod,
    val rows: List<RankRow>,
    val names: Map<String, String>
)

data class RankUi(
    val loading: Boolean = false,
    val data: RankData? = null,
    val error: String = ""
)

/* ---------- পেমেন্ট রিকোয়েস্ট ডায়ালগ ---------- */

data class RequestUi(
    val avail: String,
    val min: String,
    val pmLabel: String,
    val number: String,
    val max: Double,
    val amt: String,
    val err: String = "",
    val sending: Boolean = false
)

/* ---------- "চালিয়ে যান" ---------- */

data class Resume(val b: String, val s: String)

data class ResumeTarget(val book: Book, val bid: String, val stem: String)

data class StreakInfo(val today: Int, val cur: Int, val best: Int, val doneToday: Boolean)
