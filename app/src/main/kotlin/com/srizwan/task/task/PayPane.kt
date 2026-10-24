package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * পেমেন্ট ট্যাব (PayPane.jsx): মোট আয়ের টাইল, আয়ের পূর্বাভাস, পেমেন্ট রিকোয়েস্ট,
 * ইতিহাস, কিতাব অনুযায়ী আয়, সংশোধন দরকার, ভুলের হারের সতর্কতা।
 */
@Composable
fun PayPane(
    me: MeInfo?,
    settings: AppSettings,
    sum: Summary?,
    booksById: Map<String, Book>,
    proofs: List<Proof>,
    pays: List<Pay>,
    rateOf: (Book?) -> Double,
    reqBusy: Boolean,
    onRequest: () -> Unit,
    onOpenPage: (String, String) -> Unit
) {
    val s = sum
    val min = minPayout(settings)
    val ids = s?.byBook?.keys?.toList() ?: emptyList()
    val corr = if (s != null) proofs.filter { it.reviewStatus == "correction" } else emptyList()
    val hasReq = pays.any { it.status == "requested" }
    val ok = s != null && s.available >= min && s.available > 0
    val payNumber = me?.paymentNumber
    val noNumber = payNumber.isNullOrEmpty()

    var hint = ""
    if (s != null) {
        hint = if (noNumber) {
            "আগে প্রোফাইল থেকে পেমেন্ট নাম্বার দিন।"
        } else if (ok) {
            "উত্তোলনযোগ্য " + money(s.available) + " — রিকোয়েস্ট পাঠালে অ্যাডমিন অনুমোদন দিয়ে " +
                (PM[me?.paymentMethod] ?: "") + " নাম্বার " + payNumber + "-এ পাঠাবেন।"
        } else {
            "ন্যূনতম " + money(min) + " হলে রিকোয়েস্ট করা যাবে। আরও " +
                money(maxOf(0.0, min - s.available)) + " আয় দরকার।" +
                (if (hasReq) " (একটি রিকোয়েস্ট অপেক্ষমাণ আছে)" else "")
        }
    }

    // ভুলের হার সতর্কতা
    var warnReviewed = 0
    var warnPct = 0
    var showWarn = false
    if (s != null) {
        val reviewed = s.approved + s.correction
        val wp = settings.warnPct ?: 20.0
        val wm = settings.warnMin ?: 5.0
        val pct = if (reviewed > 0) (s.correction.toDouble() / reviewed * 100).roundToInt() else 0
        if (reviewed >= wm && pct >= wp) {
            showWarn = true
            warnReviewed = reviewed
            warnPct = pct
        }
    }

    val muted = AppColors.extra.muted

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showWarn && s != null) {
                item {
                    WarnBox(reviewed = warnReviewed, correction = s.correction, pct = warnPct)
                }
            }

            item { PayTiles(s) }

            if (s != null) {
                item { Forecast(s = s, min = min, rateOf = rateOf) }
            }

            // পেমেন্ট রিকোয়েস্ট
            item {
                TaskCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "পেমেন্ট রিকোয়েস্ট",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (s != null) {
                            Pill(text = "ডিফল্ট রেট " + rateTxt(rateOf(null)) + " / পেইজ", color = AppColors.extra.gold)
                        }
                    }
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = onRequest,
                        enabled = !reqBusy && (s == null || (ok && !noNumber)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💰  পেমেন্ট রিকোয়েস্ট করুন")
                    }
                }
            }

            // পেমেন্ট ইতিহাস
            item {
                TaskCard {
                    Text("পেমেন্ট ইতিহাস", fontWeight = FontWeight.Bold)
                    if (s != null) {
                        Spacer(Modifier.height(10.dp))
                        if (pays.isEmpty()) {
                            EmptyNote("এখনো কোনো পেমেন্ট ইতিহাস নেই")
                        } else {
                            pays.forEachIndexed { i, x ->
                                if (i > 0) Spacer(Modifier.height(8.dp))
                                PayHistoryItem(x)
                            }
                        }
                    }
                }
            }

            // কিতাব অনুযায়ী আয়
            item {
                TaskCard {
                    Text("কিতাব অনুযায়ী আয়", fontWeight = FontWeight.Bold)
                    Text(
                        text = buildAnnotatedString {
                            append("আয় গোনা হয় শুধু অ্যাডমিন যে পেইজ ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("অনুমোদিত") }
                            append(" করেছেন সেগুলোর।")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )
                    if (s != null) {
                        if (ids.isEmpty()) {
                            EmptyNote("এখনো কোনো পেইজ প্রুফড করেননি")
                        } else {
                            ids.forEachIndexed { i, id ->
                                val x = s.byBook[id] ?: return@forEachIndexed
                                if (i > 0) Spacer(Modifier.height(12.dp))
                                BookEarnItem(booksById[id], x, rateOf)
                            }
                        }
                    }
                }
            }

            // সংশোধন দরকার
            if (corr.isNotEmpty()) {
                item {
                    TaskCard {
                        Text("সংশোধন দরকার", fontWeight = FontWeight.Bold)
                        Text(
                            text = buildAnnotatedString {
                                append("অ্যাডমিন এই পেইজগুলো সংশোধন করতে বলেছেন। ঠিক করে আবার ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("প্রুফড") }
                                append(" চাপলে পুনরায় রিভিউতে যাবে।")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )
                        corr.take(50).forEachIndexed { i, p ->
                            if (i > 0) Spacer(Modifier.height(8.dp))
                            CorrectionItem(p, booksById[p.bookId], onOpenPage)
                        }
                        if (corr.size > 50) {
                            Text(
                                text = "আরও " + bn(corr.size - 50) + "টি আছে…",
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.extra.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp, horizontal = 6.dp)
    )
}

@Composable
private fun WarnBox(reviewed: Int, correction: Int, pct: Int) {
    val bad = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bad.copy(alpha = 0.12f))
            .border(1.dp, bad.copy(alpha = 0.5f), shape)
            .padding(14.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("সতর্কতা: ") }
                append(
                    "রিভিউ হওয়া " + bn(reviewed) + "টি পেইজের মধ্যে " + bn(correction) +
                        "টিতে ভুল ধরা পড়েছে (" + bn(pct) + "%)। দয়া করে মনোযোগ দিয়ে প্রুফ করুন — ভুল পেইজে আয় গোনা হয় না।"
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = bad
        )
    }
}

/** ছয়টি টাইল (২ কলাম × ৩ সারি); তথ্য আসার আগে স্কেলিটন */
@Composable
private fun PayTiles(s: Summary?) {
    if (s == null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonTile(Modifier.weight(1f))
                    SkeletonTile(Modifier.weight(1f))
                }
            }
        }
        return
    }
    val tiles = listOf(
        Triple(money(s.earned), "মোট আয় (অনুমোদিত " + bn(s.approved) + " পেইজ)", true),
        Triple(money(s.available), "উত্তোলনযোগ্য (বাকি)", false),
        Triple(money(s.paid), "এ পর্যন্ত পেয়েছেন", false),
        Triple(money(s.requested), "রিকোয়েস্টে আছে (অপেক্ষমাণ)", false),
        Triple(bn(s.pending), "রিভিউ বাকি পেইজ (" + money(s.pendingAmt) + ")", false),
        Triple(bn(s.correction), "সংশোধন দরকার", false)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (r in 0 until 3) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (c in 0 until 2) {
                    val t = tiles[r * 2 + c]
                    PayTile(
                        number = t.first,
                        label = t.second,
                        highlight = t.third,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PayTile(number: String, label: String, highlight: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val gold = AppColors.extra.gold
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (highlight) gold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (highlight) gold.copy(alpha = 0.6f) else AppColors.extra.line, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = number,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) gold else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.extra.muted
        )
    }
}

@Composable
private fun SkeletonTile(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val bar = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.extra.line, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
    }
}

private fun pagesToReach(gap: Double, rate: Double): Long {
    if (rate <= 0.0) return 0L
    val v = ceil(gap / rate)
    return if (v.isFinite() && v > 0) v.toLong() else 0L
}

/** আয়ের পূর্বাভাস: রিভিউ বাকি পেইজ অনুমোদিত হলে কত যোগ হবে */
@Composable
private fun Forecast(s: Summary, min: Double, rateOf: (Book?) -> Double) {
    val reviewedN = s.approved + s.correction
    val apr: Double? = if (reviewedN >= 3) s.approved.toDouble() / reviewedN else null
    val dr = rateOf(null)
    val allEarn = r2(s.earned + s.pendingAmt)
    val allAvail = r2(s.available + s.pendingAmt)
    val muted = AppColors.extra.muted
    val ok = AppColors.extra.ok

    TaskCard {
        Text("আয়ের পূর্বাভাস", fontWeight = FontWeight.Bold)
        if (s.pending > 0) {
            val tip = if (s.available >= min) {
                "এখনই রিকোয়েস্ট করার মতো টাকা আছে।"
            } else if (allAvail >= min) {
                "রিভিউ হয়ে গেলে ন্যূনতম পেমেন্ট (" + money(min) + ") ছুঁয়ে রিকোয়েস্ট করতে পারবেন।"
            } else {
                "ন্যূনতম পেমেন্ট (" + money(min) + ") ছুঁতে আরও ≈ " +
                    bn(pagesToReach(min - allAvail, dr)) + " পেইজ প্রুফ করতে হবে।"
            }
            Spacer(Modifier.height(4.dp))
            ForecastRow(
                label = "রিভিউ বাকি " + bn(s.pending) + " পেইজ সব অনুমোদিত হলে যোগ হবে",
                value = "≈ +" + money(s.pendingAmt),
                valueColor = ok
            )
            ForecastRow(label = "তখন মোট আয় হবে", value = "≈ " + money(allEarn))
            ForecastRow(label = "তখন উত্তোলনযোগ্য হবে", value = "≈ " + money(allAvail))
            if (apr != null && apr < 1) {
                ForecastRow(
                    label = "আপনার অনুমোদনের হার " + bn((apr * 100).roundToInt()) + "% — সেই হিসাবে যোগ হবে",
                    value = "≈ +" + money(s.pendingAmt * apr)
                )
            }
            Text(
                text = "$tip অ্যাডমিন অনুমোদন না দেওয়া পর্যন্ত এটি শুধু আনুমানিক হিসাব।",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            val extra = if (s.available < min) {
                "; ন্যূনতম পেমেন্ট (" + money(min) + ") ছুঁতে আরও ≈ " +
                    bn(pagesToReach(min - s.available, dr)) + " পেইজ দরকার"
            } else ""
            Text(
                text = "এখন রিভিউ বাকি কোনো পেইজ নেই। আরও পেইজ প্রুফড করলে অনুমোদিত হওয়ার পর প্রতি পেইজে ≈ " +
                    rateTxt(dr) + " যোগ হবে" + extra + "।",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ForecastRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
        HorizontalDivider(color = AppColors.extra.line)
    }
}

@Composable
private fun PayHistoryItem(x: Pay) {
    val ok = AppColors.extra.ok
    val bad = MaterialTheme.colorScheme.error
    val warn = AppColors.extra.warn
    val (label, color) = when (x.status) {
        "paid" -> "পরিশোধিত" to ok
        "rejected" -> "বাতিল" to bad
        else -> "অপেক্ষমাণ (বাকি)" to warn
    }
    val shape = RoundedCornerShape(12.dp)
    val method = PM[x.method] ?: x.method ?: ""
    val lines = ArrayList<String>()
    lines.add("রিকোয়েস্ট: " + fmtDT(x.createdAt))
    if (!x.processedAt.isNullOrEmpty()) {
        lines.add((if (x.status == "paid") "পরিশোধ" else "প্রক্রিয়া") + ": " + fmtDT(x.processedAt))
    }
    lines.add(
        "$method " + (x.number ?: "") +
            (if (x.pages != null && x.pages != 0) " • " + bn(x.pages) + " পেইজ" else "")
    )
    if (!x.note.isNullOrEmpty()) lines.add("নোট: " + x.note)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, AppColors.extra.line, shape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = money(x.amount),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Pill(text = label, color = color)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = lines.joinToString("\n"),
            fontSize = 12.5.sp,
            color = AppColors.extra.muted,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SegBar(a: Int, p: Int, c: Int, tot: Int) {
    val t = maxOf(tot, a + p + c, 1).toFloat()
    val rest = 1f - (a + p + c) / t
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (a > 0) Box(Modifier.weight(a / t).fillMaxHeight().background(AppColors.extra.ok))
        if (p > 0) Box(Modifier.weight(p / t).fillMaxHeight().background(AppColors.extra.gold))
        if (c > 0) Box(Modifier.weight(c / t).fillMaxHeight().background(MaterialTheme.colorScheme.error))
        if (rest > 0.0001f) Spacer(Modifier.weight(rest))
    }
}

@Composable
private fun BookEarnItem(b: Book?, x: BookSum, rateOf: (Book?) -> Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = b?.name ?: "কিতাব",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(text = money(x.earned), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        SegBar(x.approved, x.pending, x.correction, x.n)
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                text = "অনুমোদিত " + bn(x.approved) + " • বাকি " + bn(x.pending) + " • সংশোধন " + bn(x.correction),
                fontSize = 11.5.sp,
                color = AppColors.extra.muted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "রেট " + rateTxt(rateOf(b)),
                fontSize = 11.5.sp,
                color = AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun CorrectionItem(p: Proof, b: Book?, onOpenPage: (String, String) -> Unit) {
    val bad = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, AppColors.extra.line, shape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = (b?.name ?: "কিতাব") + " — " + p.stem,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Pill(
                text = "খুলুন",
                color = bad,
                modifier = Modifier.clickable { onOpenPage(p.bookId, p.stem) }
            )
        }
        if (!p.reviewNote.isNullOrEmpty()) {
            Text(
                text = "অ্যাডমিন: " + p.reviewNote,
                fontSize = 12.5.sp,
                color = AppColors.extra.muted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
