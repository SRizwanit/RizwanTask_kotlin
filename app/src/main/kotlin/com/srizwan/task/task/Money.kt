package com.srizwan.task.task

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/* common.js-এর বাংলা সংখ্যা/টাকা/আয়ের হিসাব — হুবহু একই নিয়ম */

const val DEF_RATE = 0.2
const val DEF_MIN = 50.0

val PM: Map<String, String> = mapOf("bkash" to "বিকাশ", "nagad" to "নগদ")
val KIND: Map<String, String> = mapOf("notice" to "ঘোষণা", "review" to "রিভিউ", "payment" to "পেমেন্ট")
val BADGES: List<Pair<Int, String>> = listOf(
    3 to "শুরু", 7 to "এক সপ্তাহ", 15 to "পনেরো দিন",
    30 to "এক মাস", 60 to "দুই মাস", 100 to "শতক"
)

private fun toBnDigits(s: String): String {
    val sb = StringBuilder(s.length)
    for (c in s) sb.append(if (c in '0'..'9') '০' + (c - '0') else c)
    return sb.toString()
}

/** ভারতীয়/বাংলাদেশি ল্যাখ গ্রুপিং: ১,২৩,৪৫৬ */
private fun groupLakh(digits: String): String {
    if (digits.length <= 3) return digits
    val last3 = digits.takeLast(3)
    var rest = digits.dropLast(3)
    val parts = ArrayList<String>()
    while (rest.length > 2) {
        parts.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) parts.add(0, rest)
    return parts.joinToString(",") + "," + last3
}

/** App.bn: Number(n).toLocaleString('bn-BD') */
fun bn(n: Long): String {
    val neg = n < 0
    val g = groupLakh(abs(n).toString())
    return toBnDigits((if (neg) "-" else "") + g)
}

fun bn(n: Int): String = bn(n.toLong())

fun r2(x: Double): Double {
    if (x.isNaN() || x.isInfinite()) return 0.0
    return (x * 100).roundToLong() / 100.0
}

/** App.money: ১,২৩৪.৫০ ৳ */
fun money(x: Double): String {
    val v = r2(x)
    val neg = v < 0
    val s = String.format(Locale.US, "%.2f", abs(v))
    val ip = s.substringBefore('.')
    val fp = s.substringAfter('.')
    return toBnDigits((if (neg) "-" else "") + groupLakh(ip) + "." + fp) + " ৳"
}

/** helpers.js rateTxt */
fun rateTxt(r: Double): String = money(r)

/** helpers.js minPayout */
fun minPayout(s: AppSettings): Double = s.minPayout?.takeIf { it.isFinite() } ?: DEF_MIN

private val BN_MONTHS = arrayOf(
    "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

/** App.fmtDT: ২৪ অক্টো, ৯:১২ AM */
fun fmtDT(t: String?): String {
    val ms = parseTime(t) ?: return "-"
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val h24 = z.hour
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    val mm = z.minute.toString().padStart(2, '0')
    val ap = if (h24 < 12) "AM" else "PM"
    return bn(z.dayOfMonth) + " " + BN_MONTHS[z.monthValue - 1] + ", " +
        toBnDigits("$h12:$mm") + " " + ap
}

/** App.rateOf: কিতাবের page_rate → সেটিংসের page_rate → ডিফল্ট */
fun rateOf(book: Book?, s: AppSettings): Double {
    val r = book?.pageRate
    if (r != null && r.isFinite()) return r
    val d = s.pageRate
    return if (d != null && d.isFinite()) d else DEF_RATE
}

/**
 * App.summarize: আয় গোনা হয় শুধু "অনুমোদিত" পেইজে (অনুমোদনের সময়ের রেট proof.rate-এ);
 * অপেক্ষমাণ আলাদা; "সংশোধন দরকার"-এ আয় নেই।
 */
fun summarize(
    proofs: List<Proof>,
    booksById: Map<String, Book>,
    s: AppSettings,
    pays: List<Pay>
): Summary {
    var pages = 0
    var approved = 0
    var pending = 0
    var correction = 0
    var earned = 0.0
    var pendingAmt = 0.0
    var paid = 0.0
    var requested = 0.0
    val byBook = LinkedHashMap<String, BookSum>()

    for (p in proofs) {
        val cur = rateOf(booksById[p.bookId], s)
        val old = byBook[p.bookId] ?: BookSum()
        pages++
        when (p.reviewStatus) {
            "approved" -> {
                val r = p.rate ?: cur
                approved++
                earned += r
                byBook[p.bookId] = old.copy(n = old.n + 1, approved = old.approved + 1, earned = old.earned + r)
            }
            "correction" -> {
                correction++
                byBook[p.bookId] = old.copy(n = old.n + 1, correction = old.correction + 1)
            }
            else -> {
                pending++
                pendingAmt += cur
                byBook[p.bookId] = old.copy(n = old.n + 1, pending = old.pending + 1)
            }
        }
    }
    for (x in pays) {
        if (x.status == "paid") paid += x.amount
        else if (x.status == "requested") requested += x.amount
    }
    val e = r2(earned)
    val pa = r2(paid)
    val rq = r2(requested)
    return Summary(
        pages = pages,
        approved = approved,
        pending = pending,
        correction = correction,
        earned = e,
        pendingAmt = r2(pendingAmt),
        paid = pa,
        requested = rq,
        available = maxOf(0.0, r2(e - pa - rq)),
        byBook = byBook
    )
}
