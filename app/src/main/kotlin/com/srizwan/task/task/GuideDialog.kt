package com.srizwan.task.task

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.drawscope.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.srizwan.task.AppColors
import com.srizwan.task.AppFonts
import com.srizwan.task.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.cos

/* GuideDialog.jsx + guideScenes.js:
   প্রথমবারের গাইড — ধাপে ধাপে অ্যানিমেটেড ডায়ালগ। React-এর SVG/Remotion-স্টাইল অ্যানিমেশন Compose Canvas-এ
   ফ্রেম-ভিত্তিক (৩০ FPS) হুবহু পোর্ট করা: interpolate/spring একই সূত্রে, দৃশ্য ও সময় একই। */

private const val FPS = 30
private const val PHASE_ALL = -2   // কমানো-মোশনে সব উপ-ধাপ হাইলাইট
private const val PHASE_NONE = -1

/* ---------- Remotion-এর interpolate / spring / easing ---------- */

private val EaseOut: (Float) -> Float = { t -> 1f - (1f - t).pow(3) }
private val EaseInOut: (Float) -> Float = { t ->
    if (t < .5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
}

private fun interp(f: Float, inR: FloatArray, outR: FloatArray, ease: ((Float) -> Float)? = null): Float {
    val n = inR.size
    if (f <= inR[0]) return outR[0]
    if (f >= inR[n - 1]) return outR[n - 1]
    var i = 0
    while (i < n - 2 && f > inR[i + 1]) i++
    var t = (f - inR[i]) / (if (inR[i + 1] - inR[i] == 0f) 1f else inR[i + 1] - inR[i])
    if (ease != null) t = ease(t)
    return outR[i] + (outR[i + 1] - outR[i]) * t
}

private fun interp(f: Float, a0: Float, a1: Float, o0: Float, o1: Float, ease: ((Float) -> Float)? = null): Float =
    interp(f, floatArrayOf(a0, a1), floatArrayOf(o0, o1), ease)

private fun spring(f: Float, stiffness: Float = 110f, damping: Float = 11f, mass: Float = 1f): Float {
    val t = max(0f, f) / FPS
    val w0 = sqrt(stiffness / mass)
    val z = damping / (2f * sqrt(stiffness * mass))
    return if (z < 1f) {
        val wd = w0 * sqrt(1f - z * z)
        1f - exp(-z * w0 * t) * (cos(wd * t) + (z * w0 / wd) * sin(wd * t))
    } else {
        1f - exp(-w0 * t) * (1f + w0 * t)
    }
}

private fun fadeIO(f: Float, dur: Int): Float =
    interp(f, 0f, 8f, 0f, 1f) * interp(f, dur - 10f, dur.toFloat(), 1f, 0f)

private fun bd(s: String): String = buildString {
    for (ch in s) append(if (ch in '0'..'9') "০১২৩৪৫৬৭৮৯"[ch - '0'] else ch)
}

/* ---------- রঙ ---------- */

private val Gold = Color(0xFFF2D27A)
private val GoldDark = Color(0xFFA8761F)
private val Brown = Color(0xFF4A3200)
private val Deep = Color(0xFF0B5F4C)
private val Mint = Color(0xFF16A583)
private val Cream = Color(0xFFF7E2A0)
private val CardLine = Color(0xFFCFE3DC)
private val Track = Color(0xFFDFEAE6)

/* ---------- দৃশ্যের সরঞ্জাম ---------- */

private class Ctx(val tm: TextMeasurer, val d: Density, val font: FontFamily, val imgs: Map<String, ImageBitmap>)

private class Scene(
    val w: Float,
    val h: Float,
    val dur: Int,
    val phase: (Int) -> Int,
    val draw: DrawScope.(Ctx, Int) -> Unit
)

private data class R4(val x: Float, val y: Float, val w: Float, val h: Float)

/** SVG-র `<g transform="translate rotate scale" opacity>` */
private fun DrawScope.grp(
    x: Float, y: Float, s: Float = 1f, rot: Float = 0f, alpha: Float = 1f,
    block: DrawScope.() -> Unit
) {
    withTransform({
        translate(x, y)
        rotate(rot, Offset.Zero)
        scale(s, s, Offset.Zero)
    }) {
        layer(alpha, block)
    }
}

/** গ্রুপের opacity */
private fun DrawScope.layer(alpha: Float, block: DrawScope.() -> Unit) {
    val a = alpha.coerceIn(0f, 1f)
    if (a >= 0.999f) {
        block()
        return
    }
    if (a <= 0.001f) return
    drawIntoCanvas { cv ->
        cv.saveLayer(Rect(-20000f, -20000f, 20000f, 20000f), Paint().apply { this.alpha = a })
    }
    block()
    drawIntoCanvas { it.restore() }
}

/** y = লেখার উল্লম্ব মাঝ। anchor: 0 = শুরু, 1 = মাঝ */
private fun DrawScope.txt(
    cx: Ctx, s: String, x: Float, y: Float, size: Float, color: Color,
    bold: Boolean = false, anchor: Int = 1
) {
    val style = TextStyle(
        fontFamily = cx.font,
        fontSize = with(cx.d) { size.toSp() },
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color
    )
    val r = cx.tm.measure(s, style)
    val left = if (anchor == 1) x - r.size.width / 2f else x
    drawText(r, topLeft = Offset(left, y - r.size.height / 2f))
}

private fun DrawScope.rrect(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color, alpha: Float = 1f) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r), alpha = alpha)

private fun DrawScope.rrectStroke(
    x: Float, y: Float, w: Float, h: Float, r: Float, color: Color, sw: Float, alpha: Float = 1f
) = drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r), style = Stroke(sw), alpha = alpha)

private fun DrawScope.bg(w: Float, h: Float, color: Color) = drawRect(color, Offset.Zero, Size(w, h))

private fun DrawScope.bgGrad(w: Float, h: Float, a: Color, b: Color) =
    drawRect(Brush.linearGradient(listOf(a, b), start = Offset.Zero, end = Offset(w, h)), Offset.Zero, Size(w, h))

/* আঙুলের স্পর্শ (touch dot + ripple) */
private fun DrawScope.touch(
    f: Float, fromX: Float, fromY: Float, toX: Float, toY: Float,
    t0: Float, t1: Float, tp: Float, r: Float, op: Float
) {
    val m = interp(f, t0, t1, 0f, 1f, EaseInOut)
    val x = fromX + (toX - fromX) * m
    val y = fromY + (toY - fromY) * m
    val s = interp(f, floatArrayOf(tp, tp + 4f, tp + 9f), floatArrayOf(1f, .78f, 1f))
    val dotOp = op * interp(f, t0 - 6f, t0, 0f, 1f)
    val rf = interp(f, tp + 2f, tp + 28f, 0f, 1f, EaseOut)
    if (f >= tp + 2f) {
        drawCircle(Color.White, r * (.5f + 2.2f * rf), Offset(toX, toY), alpha = (1f - rf) * .9f * op, style = Stroke(2.4f))
    }
    grp(x, y, s, alpha = dotOp) {
        drawCircle(Color.White.copy(alpha = .92f), r * .55f, Offset.Zero)
        drawCircle(Deep, r * .55f, Offset.Zero, style = Stroke(2f))
        drawCircle(Deep, r * .18f, Offset.Zero)
    }
}

/* ---------- স্ক্রিনশট-ভিত্তিক দৃশ্য: স্পটলাইট + রিং + নম্বর ব্যাজ + আঙুল ---------- */

private fun shot(
    key: String, W: Float, H: Float, T: List<R4>, D: Int = 74, swapKey: String? = null, swapAt: Int = 0
): Scene {
    val n = T.size
    val total = n * D + 34
    val sw = W / 120f
    val pad = W / 90f
    val R = W / 32f
    return Scene(W, H, total, { f -> min(n - 1, f / D) }) { cx, fi ->
        val f = fi.toFloat()
        val idx = min(n - 1, fi / D)
        val lf = f - idx * D
        val c = T[idx]
        val dst = IntSize(W.roundToInt(), H.roundToInt())
        drawImage(cx.imgs.getValue(key), dstSize = dst)
        if (swapKey != null) {
            val a = if (idx > swapAt) 1f else if (idx == swapAt) interp(lf, 24f, 38f, 0f, 1f) else 0f
            if (a > 0f) drawImage(cx.imgs.getValue(swapKey), dstSize = dst, alpha = a)
        }
        val pv = if (idx > 0) T[idx - 1] else R4(c.x - W * .04f, c.y - W * .04f, c.w + W * .08f, c.h + W * .08f)
        val m = interp(lf, 0f, 16f, 0f, 1f, EaseInOut)
        fun L(a: Float, b: Float) = a + (b - a) * m
        val x = L(pv.x, c.x) - pad
        val y = L(pv.y, c.y) - pad
        val w = L(pv.w, c.w) + 2 * pad
        val h = L(pv.h, c.h) + 2 * pad
        val rx = min(w, h) * .28f
        val op = interp(f, 0f, 10f, 0f, 1f) * interp(f, total - 12f, total.toFloat(), 1f, 0f)

        val dim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, W, H))
            addRoundRect(RoundRect(x, y, x + w, y + h, CornerRadius(rx)))
        }
        drawPath(dim, Color(0xFF03231B), alpha = .62f * op)
        rrectStroke(x, y, w, h, rx, Gold, sw, alpha = op * (.78f + .22f * sin(f * .28f)))

        grp(x + rx * .2f, y + rx * .2f, max(0f, spring(lf - 6f, stiffness = 170f, damping = 9f)), alpha = op) {
            drawCircle(Gold, R * .62f, Offset.Zero)
            drawCircle(Brown, R * .62f, Offset.Zero, style = Stroke(sw * .4f))
            txt(cx, bd((idx + 1).toString()), 0f, 0f, R * .8f, Brown, bold = true)
        }

        val ccx = c.x + c.w * .62f
        val ccy = c.y + c.h * .7f
        val pcx: Float
        val pcy: Float
        if (idx > 0) {
            pcx = T[idx - 1].x + T[idx - 1].w * .62f
            pcy = T[idx - 1].y + T[idx - 1].h * .7f
        } else {
            pcx = ccx + W * .22f
            pcy = ccy + H * .9f
        }
        val dm = interp(lf, 0f, 18f, 0f, 1f, EaseInOut)
        grp(
            pcx + (ccx - pcx) * dm, pcy + (ccy - pcy) * dm,
            interp(lf, floatArrayOf(18f, 22f, 27f), floatArrayOf(1f, .78f, 1f)), alpha = op
        ) {
            drawCircle(Color.White.copy(alpha = .92f), R * .55f, Offset.Zero)
            drawCircle(Deep, R * .55f, Offset.Zero, style = Stroke(sw * .8f))
            drawCircle(Deep, R * .18f, Offset.Zero)
        }
        val rf = interp(lf, 20f, 46f, 0f, 1f, EaseOut)
        if (lf >= 20f) {
            drawCircle(Color.White, R * (.5f + 2.2f * rf), Offset(ccx, ccy), alpha = (1f - rf) * .9f * op, style = Stroke(sw * 1.2f))
        }
    }
}

/* ---------- ভেক্টর দৃশ্য ১: স্বাগতম ---------- */

private fun sceneWelcome(): Scene {
    val dur = 150
    val stars = listOf(
        40f to 30f, 90f to 72f, 150f to 24f, 250f to 42f, 372f to 112f,
        60f to 150f, 340f to 172f, 300f to 18f, 120f to 160f
    )
    return Scene(400f, 200f, dur, { PHASE_NONE }) { cx, fi ->
        val f = fi.toFloat()
        bgGrad(400f, 200f, Color(0xFF0A4F40), Mint)
        layer(fadeIO(f, dur)) {
            stars.forEachIndexed { i, s ->
                val a = .3f + .7f * (.5f + .5f * sin(f / 7f + i * 1.7f))
                drawCircle(Cream, 1.5f + (i % 3) * .55f, Offset(s.first, s.second), alpha = a)
            }
            val moon = Path.combine(
                PathOperation.Difference,
                Path().apply { addOval(Rect(Offset(338f, 46f), 21f)) },
                Path().apply { addOval(Rect(Offset(347f, 38f), 17f)) }
            )
            drawPath(moon, Gold)

            val s = .55f + .45f * spring(f, stiffness = 90f, damping = 10f)
            grp(200f, 112f + sin(f / 14f) * 3f, s) {
                rrect(-92f, -44f, 184f, 86f, 10f, Color(0xFF073B30))
                val left = Path().apply {
                    moveTo(-4f, -36f); cubicTo(-28f, -44f, -60f, -42f, -84f, -34f)
                    lineTo(-84f, 38f); cubicTo(-60f, 30f, -28f, 28f, -4f, 36f); close()
                }
                val right = Path().apply {
                    moveTo(4f, -36f); cubicTo(28f, -44f, 60f, -42f, 84f, -34f)
                    lineTo(84f, 38f); cubicTo(60f, 30f, 28f, 28f, 4f, 36f); close()
                }
                drawPath(left, Color.White)
                drawPath(right, Color(0xFFF8F3E3))
                drawRect(Color(0xFFC9972B), Offset(-1.5f, -36f), Size(3f, 72f))
                for (i in 0..3) {
                    val w = interp(f, 24f + i * 9f, 42f + i * 9f, 0f, 52f - (i % 2) * 16f)
                    rrect(-68f, -20f + i * 13f, w, 4f, 2f, Deep, .38f)
                    rrect(16f, -20f + i * 13f, w * .92f, 4f, 2f, Deep, .38f)
                }
            }
            grp(262f, 66f, max(0f, spring(f - 58f, stiffness = 140f, damping = 8f))) {
                drawCircle(Gold, 17f, Offset.Zero)
                drawCircle(Color.White, 17f, Offset.Zero, style = Stroke(3f))
                val ck = Path().apply { moveTo(-7f, 0f); lineTo(-2f, 6f); lineTo(8f, -5f) }
                drawPath(ck, Brown, style = Stroke(3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/* ---------- ভেক্টর দৃশ্য ২: কিতাব বেছে নেওয়া ---------- */

private fun sceneBook(): Scene {
    val dur = 140
    val cols = listOf(Color(0xFF0B5F4C), Color(0xFFA8761F), Color(0xFF2C6E8F))
    val cxs = floatArrayOf(80f, 200f, 320f)
    val pr = floatArrayOf(.62f, .35f, .8f)
    return Scene(400f, 200f, dur, { PHASE_NONE }) { cx, fi ->
        val f = fi.toFloat()
        bg(400f, 200f, Color(0xFFE8F4EF))
        val op = fadeIO(f, dur)
        val lift = max(0f, spring(f - 58f, stiffness = 160f, damping = 9f))
        layer(op) {
            for (i in 0..2) {
                val sc = if (i == 1) 1f + .07f * lift else 1f
                val y = 98f - (if (i == 1) 6f * lift else 0f)
                grp(cxs[i], y, sc) {
                    rrect(-52f, -72f, 104f, 144f, 11f, Color.White)
                    rrectStroke(-52f, -72f, 104f, 144f, 11f, CardLine, 1f)
                    rrect(-44f, -64f, 88f, 62f, 7f, cols[i])
                    rrectStroke(-14f, -46f, 28f, 30f, 0f, Cream, 2.4f)
                    drawLine(Cream, Offset(0f, -46f), Offset(0f, -16f), 2.4f)
                    rrect(-44f, 6f, 78f, 6f, 3f, Color(0xFFCFD8D4))
                    rrect(-44f, 18f, 52f, 5f, 2.5f, Color(0xFFE3EAE7))
                    rrect(-44f, 36f, 88f, 7f, 3.5f, Color(0xFFDFEAE6))
                    rrect(-44f, 36f, interp(f, 8f + i * 4f, 44f + i * 4f, 0f, 88f * pr[i], EaseOut), 7f, 3.5f, Mint)
                    rrect(-44f, 50f, 60f, 5f, 2.5f, Color(0xFFE3B94F), .75f)
                }
            }
            val sa = if (f >= 58f) min(1f, (f - 58f) / 6f) else 0f
            grp(cxs[1], 92f, 1f + .07f * lift, alpha = sa) {
                rrectStroke(-56f, -76f, 112f, 152f, 14f, Gold, 3.5f)
            }
            grp(0f, 0f, alpha = interp(f, 74f, 84f, 0f, 1f)) {
                rrect(100f, 176f, 200f, 20f, 10f, Gold)
                txt(cx, "কিতাব খুলছে…", 200f, 186.5f, 11f, Brown, bold = true)
            }
            touch(f, 350f, 196f, 200f, 84f, 28f, 52f, 56f, 20f, op)
        }
    }
}

/* ---------- ভেক্টর দৃশ্য ৩: রিভিউ, আয় ও পেমেন্ট রিকোয়েস্ট ---------- */

private fun sceneEarn(): Scene {
    val dur = 250
    val rowsY = floatArrayOf(14f, 64f, 114f)
    val rowsT = listOf("পেইজ ১২", "পেইজ ১৩", "পেইজ ১৪")
    val rowsOk = booleanArrayOf(true, true, false)
    val st = floatArrayOf(36f, 62f, 88f)
    val arr = floatArrayOf(70f, 96f)
    return Scene(400f, 200f, dur, { f -> if (f < 84) 0 else if (f < 150) 1 else 2 }) { cx, fi ->
        val f = fi.toFloat()
        bg(400f, 200f, Color(0xFFEEF7F3))
        val op = fadeIO(f, dur)
        layer(op) {
            for (i in 0..2) {
                val ap = interp(f, 6f + i * 10f, 20f + i * 10f, 0f, 1f, EaseOut)
                val y = rowsY[i]
                grp((1f - ap) * -30f, 0f, alpha = ap) {
                    rrect(14f, y, 184f, 42f, 10f, Color.White)
                    rrectStroke(14f, y, 184f, 42f, 10f, CardLine, 1f)
                    rrect(27f, y + 9f, 17f, 24f, 3f, Color(0xFFE8F1ED))
                    rrectStroke(27f, y + 9f, 17f, 24f, 3f, Color(0xFF9DB8AF), 1f)
                    rrect(31f, y + 16f, 9f, 2.5f, 1.2f, Color(0xFF9DB8AF))
                    rrect(31f, y + 22f, 9f, 2.5f, 1.2f, Color(0xFF9DB8AF))
                    txt(cx, rowsT[i], 54f, y + 21f, 13f, Color(0xFF12372F), bold = true, anchor = 0)
                    val sp = spring(f - st[i], stiffness = 220f, damping = 7f)
                    val ss = if (f >= st[i]) 1f + .9f * (1f - min(1f, sp)) else 2f
                    val sa = if (f >= st[i]) min(1f, (f - st[i]) / 3f) else 0f
                    val edge = if (rowsOk[i]) Mint else Color(0xFFD64545)
                    grp(150f, y + 21f, ss, rot = -9f, alpha = sa) {
                        rrect(-33f, -12f, 66f, 24f, 6f, Color.White)
                        rrectStroke(-33f, -12f, 66f, 24f, 6f, edge, 2.4f)
                        txt(
                            cx, if (rowsOk[i]) "অনুমোদিত" else "সংশোধন", 0f, 0.5f, 11.5f,
                            if (rowsOk[i]) Color(0xFF0D7A5F) else Color(0xFFC0392B), bold = true
                        )
                    }
                }
            }
            var earned = 0f
            for (i in 0..1) {
                val t = interp(f, st[i] + 10f, arr[i], 0f, 1f, EaseInOut)
                val x = 205f + (297f - 205f) * t
                val y = rowsY[i] + 21f + (52f - rowsY[i] - 21f) * t - 34f * sin(PI.toFloat() * t)
                if (f > st[i] + 10f && f < arr[i]) {
                    grp(x, y) {
                        drawCircle(Gold, 8f, Offset.Zero)
                        drawCircle(GoldDark, 8f, Offset.Zero, style = Stroke(1.6f))
                        drawCircle(GoldDark, 4.4f, Offset.Zero, style = Stroke(1.2f))
                    }
                }
                if (f >= arr[i]) earned += .2f
            }
            val bump = max(spring(f - arr[0], stiffness = 260f, damping = 6f), 0f)
            val bump2 = max(spring(f - arr[1], stiffness = 260f, damping = 6f), 0f)
            val pulse = 1f +
                .1f * sin(PI.toFloat() * min(1f, bump)) * (if (f < arr[1]) 1f else 0f) +
                .1f * sin(PI.toFloat() * min(1f, bump2)) * (if (f >= arr[1]) 1f else 0f)
            grp(297f, 58f, pulse) {
                rrect(-36f, -22f, 72f, 46f, 9f, Deep)
                drawLine(Gold, Offset(-36f, -8f), Offset(36f, -8f), 2.2f, alpha = .7f)
                rrect(12f, -2f, 24f, 16f, 6f, Color(0xFF0A4F40))
                rrectStroke(12f, -2f, 24f, 16f, 6f, Gold, 2f)
                drawCircle(Gold, 3.2f, Offset(23f, 6f))
            }
            txt(cx, bd(String.format(java.util.Locale.US, "%.2f", earned)) + " ৳", 297f, 103f, 25f, Color(0xFF0B4F40), bold = true)
            txt(cx, "মোট আয়", 297f, 126f, 11f, Color(0xFF59706A))

            val bp = max(spring(f - 140f, stiffness = 120f, damping = 11f), 0f)
            val pressed = f >= 186f
            grp(297f, 162f + (1f - min(1f, bp)) * 34f, alpha = min(1f, bp)) {
                rrect(-76f, -18f, 152f, 36f, 11f, if (pressed) Mint else Deep)
                txt(
                    cx, if (pressed) "✓ রিকোয়েস্ট পাঠানো হয়েছে" else "পেমেন্ট রিকোয়েস্ট",
                    0f, 1f, if (pressed) 11f else 12.5f, Color.White, bold = true
                )
            }
            touch(f, 392f, 196f, 338f, 170f, 158f, 180f, 184f, 18f, op)
        }
    }
}

/* ---------- ভেক্টর দৃশ্য: দৈনিক লক্ষ্য, টানা দিন ও চালিয়ে যান ---------- */

private fun sceneGoal(): Scene {
    val dur = 180
    return Scene(400f, 200f, dur, { f -> if (f < 56) 0 else if (f < 112) 1 else 2 }) { cx, fi ->
        val f = fi.toFloat()
        bg(400f, 200f, Color(0xFFE8F4EF))
        val op = fadeIO(f, dur)
        layer(op) {
            val n = interp(f, 6f, 50f, 0f, 14f, EaseOut).roundToInt()
            // কার্ড ১: আজকের লক্ষ্য
            rrect(22f, 16f, 176f, 92f, 14f, Color.White)
            rrectStroke(22f, 16f, 176f, 92f, 14f, CardLine, 1f)
            txt(cx, "আজকের লক্ষ্য", 38f, 36f, 12f, Deep, bold = true, anchor = 0)
            txt(cx, bd(n.toString()), 38f, 64f, 28f, Deep, bold = true, anchor = 0)
            txt(cx, "/ ২০ পেইজ", 72f, 69f, 14f, Color(0xFF6B7280), anchor = 0)
            rrect(38f, 86f, 144f, 8f, 4f, Track)
            rrect(38f, 86f, interp(f, 6f, 50f, 0f, 144f * 14f / 20f, EaseOut), 8f, 4f, Mint)
            // কার্ড ২: টানা কাজ
            rrect(212f, 16f, 166f, 92f, 14f, Color.White)
            rrectStroke(212f, 16f, 166f, 92f, 14f, CardLine, 1f)
            txt(cx, "টানা কাজ", 228f, 36f, 12f, GoldDark, bold = true, anchor = 0)
            val streak = interp(f, 58f, 90f, 0f, 7f, EaseOut).roundToInt()
            txt(cx, bd(streak.toString()), 228f, 64f, 28f, GoldDark, bold = true, anchor = 0)
            txt(cx, "দিন", 258f, 69f, 14f, Color(0xFF6B7280), anchor = 0)
            for (i in 0..2) {
                drawCircle(if (f > 74f + i * 10f) Gold else Track, 7f, Offset(236f + i * 28f, 92f))
            }
            // চালিয়ে যান বোতাম
            val pop = 1f + .04f * max(0f, spring(f - 114f, stiffness = 170f, damping = 8f))
            withTransform({ scale(pop, pop, Offset(200f, 150f)) }) {
                rrect(22f, 124f, 356f, 52f, 26f, Deep)
                val tri = Path().apply { moveTo(118f, 142f); lineTo(132f, 150f); lineTo(118f, 158f); close() }
                drawPath(tri, Cream)
                txt(cx, "চালিয়ে যান", 214f, 150.5f, 16f, Cream, bold = true)
            }
            touch(f, 350f, 196f, 250f, 150f, 112f, 136f, 140f, 18f, op)
        }
    }
}

/* ---------- ভেক্টর দৃশ্য ৪: র‍্যাংক, নোটিশ, টিকেট, প্রোফাইল ---------- */

private fun sceneCommunity(): Scene {
    val dur = 190
    val cxs = floatArrayOf(50f, 150f, 250f, 350f)
    val labels = listOf("র‍্যাংক", "নোটিশ", "টিকেট", "প্রোফাইল")
    val st = floatArrayOf(6f, 46f, 90f, 134f)
    val hs = floatArrayOf(34f, 50f, 24f)
    return Scene(400f, 200f, dur, { f -> if (f < 44) 0 else if (f < 88) 1 else if (f < 132) 2 else 3 }) { cx, fi ->
        val f = fi.toFloat()
        bgGrad(400f, 200f, Color(0xFF0A4F40), Color(0xFF127A63))
        val ph = if (fi < 44) 0 else if (fi < 88) 1 else if (fi < 132) 2 else 3
        layer(fadeIO(f, dur)) {
            for (i in 0..3) {
                val s = max(0f, spring(f - st[i], stiffness = 150f, damping = 9f))
                val hi = if (ph == i) 1.1f else 1f
                grp(cxs[i], 88f, s * hi, alpha = min(1f, s * 1.2f)) {
                    when (i) {
                        0 -> {
                            val cols = listOf(Color(0xFFC9D6D1), Gold, Color(0xFFD9A066))
                            val xs = floatArrayOf(-25f, -7.5f, 10f)
                            for (k in 0..2) {
                                val h = interp(f, st[0] + 6f + k * 6f, st[0] + 26f + k * 6f, 0f, hs[k], EaseOut)
                                rrect(xs[k], 30f - h, 15f, h, 3f, cols[k])
                            }
                            drawCircle(Cream, 6f, Offset(0f, -34f))
                            drawCircle(GoldDark, 6f, Offset(0f, -34f), style = Stroke(1.6f))
                        }
                        1 -> {
                            val t = max(0f, f - st[1] - 6f) / FPS
                            val ang = if (ph == 1 || f > st[1]) 22f * exp(-t * .9f) * sin(t * 16f) else 0f
                            withTransform({ rotate(ang, Offset(0f, -31f)) }) {
                                val bell = Path().apply {
                                    moveTo(0f, -28f); cubicTo(-15f, -28f, -22f, -17f, -22f, -3f)
                                    lineTo(-22f, 12f); lineTo(-30f, 22f); lineTo(30f, 22f); lineTo(22f, 12f)
                                    lineTo(22f, -3f); cubicTo(22f, -17f, 15f, -28f, 0f, -28f); close()
                                }
                                drawPath(bell, Gold)
                                drawPath(bell, GoldDark, style = Stroke(2f, join = StrokeJoin.Round))
                                drawCircle(Gold, 3.4f, Offset(0f, -31f))
                                drawCircle(GoldDark, 3.4f, Offset(0f, -31f), style = Stroke(1.6f))
                                drawCircle(Color(0xFFC9972B), 5f, Offset(0f, 30f))
                            }
                            val da = max(0f, min(1f, spring(f - st[1] - 14f, stiffness = 200f, damping = 7f)))
                            drawCircle(Color(0xFFE5484D), 6f, Offset(16f, -22f), alpha = da)
                        }
                        2 -> {
                            rrect(-36f, -22f, 72f, 44f, 9f, Gold)
                            rrectStroke(-36f, -22f, 72f, 44f, 9f, GoldDark, 2f)
                            drawLine(
                                GoldDark, Offset(14f, -20f), Offset(14f, 20f), 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                            )
                            rrect(-26f, -10f, 32f, 5f, 2.5f, Brown, .7f)
                            rrect(-26f, 2f, 24f, 5f, 2.5f, Brown, .7f)
                            rrect(-26f, 14f, 28f, 4f, 2f, Brown, .45f)
                        }
                        else -> {
                            val rf = ((f - st[3]).mod(40f)) / 40f
                            if (f > st[3]) {
                                drawCircle(Gold, 26f + 16f * rf, Offset.Zero, alpha = (1f - rf) * .9f, style = Stroke(2.4f))
                            }
                            drawCircle(Color(0xFF0A4F40), 27f, Offset.Zero)
                            drawCircle(Gold, 27f, Offset.Zero, style = Stroke(3f))
                            drawCircle(Cream, 9f, Offset(0f, -7f))
                            val body = Path().apply {
                                moveTo(-16f, 20f); cubicTo(-16f, 8f, 16f, 8f, 16f, 20f); close()
                            }
                            drawPath(body, Cream)
                        }
                    }
                    txt(cx, labels[i], 0f, 57.5f, 13f, Cream, bold = true)
                }
            }
        }
    }
}

/* ---------- গাইডের ধাপ ---------- */

private class GuideStep(val tag: String, val t: String, val d: String, val subs: List<String>, val scene: Scene)

private val GUIDE: List<GuideStep> = listOf(
    GuideStep(
        "স্বাগতম", "আসসালামু আলাইকুম, স্বাগতম!",
        "এই অ্যাপে কিতাবের পেইজ এ আই কে দিয়ে টেক্সট করিয়ে, সে টেক্সট দেখে ভুল শুধরে প্রুফ করবেন। কাজের বিনিময়ে প্রতি অনুমোদিত পেইজে নির্দিষ্ট রেটে আয় হবে।",
        emptyList(), sceneWelcome()
    ),
    GuideStep(
        "শুরুতে", "কিতাব বেছে নিন",
        "কিতাবের কার্ডে চাপ দিন। প্রতিটি কার্ডে রেট ও অগ্রগতির বার (কত পেইজ হলো, কত বাকি) দেখা যায়।",
        emptyList(), sceneBook()
    ),
    GuideStep(
        "ধাপ ১", "ছবি ডাউনলোড করে প্রম্পট কপি করুন",
        "পেইজ খুললে উপরের টুলবারে ছবি ডাউনলোডের বোতাম ও তার পাশে এআই (✦) চিহ্ন পাবেন।",
        listOf(
            "টাস্ক থেকে ডাউনলোড বোতামে চাপ দিয়ে পেইজের ছবিটি ফোনে নামান।",
            "পাশের এআই (✦) চিহ্নে ক্লিক করুন — প্রম্পট কপি হয়ে যাবে।"
        ),
        shot("a", 400f, 124f, listOf(R4(12f, 10f, 52f, 36f), R4(133f, 10f, 52f, 36f)))
    ),
    GuideStep(
        "ধাপ ২", "ছবি ও প্রম্পট Meta AI বা ChatGPT-কে দিন",
        "meta.ai বা chatgpt.com খুলে ডাউনলোড করা ছবি এবং কপি করা প্রম্পট একসাথে পাঠান।",
        listOf(
            "\"+\" চেপে ডাউনলোড করা ছবিটি যোগ করুন।",
            "কপি করা প্রম্পট লেখার ঘরে পেস্ট করুন।",
            "পাঠানোর তীর (↑) চাপুন — Meta AI ছবি থেকে টেক্সট বের করে দেবে।"
        ),
        shot("b", 680f, 245f, listOf(R4(37f, 150f, 60f, 60f), R4(20f, 40f, 620f, 70f), R4(585f, 152f, 56f, 56f)))
    ),
    GuideStep(
        "ধাপ ৩", "AI-র দেওয়া টেক্সট কপি করুন",
        "Meta AI বা ChatGPT-এর উত্তর শেষ হলে নিচের কপি আইকনে চাপ দিন। এতে পুরো টেক্সট একবারে কপি হবে।",
        listOf("উত্তরের নিচের কপি আইকনে চাপ দিন, তারপর অ্যাপে ফিরে আসুন।"),
        shot("c", 680f, 170f, listOf(R4(112f, 110f, 56f, 56f)), D = 96)
    ),
    GuideStep(
        "ধাপ ৪", "টেক্সট বক্সে হেডিং চালু করে পেস্ট করুন",
        "অ্যাপের পেইজে ফিরে টেক্সট বক্সে কপি করা টেক্সট বসান। পেইজে শিরোনাম (হেডিং) থাকলে টেক্সট বক্সে হেডিং চালু করে নিন।",
        listOf(
            "টেক্সট বক্সের পেস্ট (📋) আইকনে চাপ দিন — কপি করা টেক্সট বসে যাবে।",
            "অক্ষর সংখ্যা দেখে নিশ্চিত হন যে টেক্সট এসেছে।",
            "টেক্সট বক্সে লেখাটি ছবির সাথে মিলছে কি না পড়ে দেখুন।"
        ),
        shot(
            "d0", 380f, 175f,
            listOf(R4(238f, 7f, 47f, 36f), R4(290f, 13f, 80f, 22f), R4(30f, 75f, 330f, 50f)),
            swapKey = "d", swapAt = 0
        )
    ),
    GuideStep(
        "ধাপ ৫", "সেভ করুন",
        "টেক্সট বসানোর পর নিচের বারের \"সেভ\" দিয়ে সংরক্ষণ করুন।",
        listOf(
            "\"সেভ\" — শুধু এই পেইজ সংরক্ষণ হবে।",
            "\"সেভ ও পরের\" — সংরক্ষণ করে সরাসরি পরের পেইজে চলে যাবে।"
        ),
        shot("e", 720f, 64f, listOf(R4(12f, 12f, 93f, 44f), R4(118f, 12f, 166f, 44f)), D = 84)
    ),
    GuideStep(
        "ধাপ ৬", "প্রুফ করে \"প্রুফড\" চিহ্ন দিন",
        "ছবি ও টেক্সট মিলিয়ে ভুল ঠিক করুন, তারপর প্রুফড চিহ্ন দিন। অ্যাডমিন পরে আপনার কাজ রিভিউ করে \"অনুমোদিত\" বা \"সংশোধন দরকার\" চিহ্ন দেবেন।",
        listOf(
            "ছবির সাথে টেক্সট মিলিয়ে ভুল শুধরে নিন।",
            "নিচের \"প্রুফড\" বক্সে টিক দিন — পেইজটি রিভিউতে যাবে।",
            "\"সেভ ও পরের\" চেপে পরের পেইজে যান।"
        ),
        shot("e", 720f, 64f, listOf(R4(294f, 12f, 112f, 44f), R4(118f, 12f, 166f, 44f)), D = 84)
    ),
    GuideStep(
        "ফলাফল", "আয় ও পেমেন্ট",
        "\"পেমেন্ট\" ট্যাবে মোট আয়, বাকি টাকা ও ইতিহাস দেখুন। আয় গোনা হয় শুধু অ্যাডমিন যে পেইজ অনুমোদিত করেছেন সেগুলোর।",
        listOf(
            "\"অনুমোদিত\" পেইজ আপনার আয়ে যোগ হয়।",
            "\"সংশোধন দরকার\" হলে ঠিক করে আবার \"প্রুফড\" চাপুন — ভুল পেইজে আয় গোনা হয় না।",
            "নির্দিষ্ট পরিমাণ হলে \"পেমেন্ট রিকোয়েস্ট\" চেপে টাকা চাইতে পারবেন।"
        ),
        sceneEarn()
    ),
    GuideStep(
        "নতুন", "লক্ষ্য, টানা দিন ও চালিয়ে যান",
        "টাস্ক ট্যাবের উপরের কার্ডে আজকের লক্ষ্য, টানা কাজের দিন ও ব্যাজ দেখা যায়। \"চালিয়ে যান\" বাটনে শেষ কাজের জায়গায় ফেরা যায়।",
        listOf(
            "লক্ষ্যের ঘরে চাপ দিয়ে দৈনিক লক্ষ্য (কত পেইজ) বদলান।",
            "প্রতিদিন অন্তত একটি পেইজ প্রুফড করলে টানা দিন বাড়ে; ৩, ৭, ১৫, ৩০ দিনে ব্যাজ মেলে।",
            "\"চালিয়ে যান\" চাপলে শেষ যে কিতাব ও পেইজে ছিলেন সেখানে সরাসরি খুলবে।"
        ),
        sceneGoal()
    ),
    GuideStep(
        "সাহায্য", "লিডারবোর্ড, নোটিশ ও সাহায্য",
        "\"র‍্যাংক\" ট্যাবে সবার মধ্যে আপনার অবস্থান দেখুন। অ্যাডমিনের ঘোষণা ডায়ালগে আসবে। উপরের টুলবারের প্রোফাইল আইকনে আপনার সব তথ্য আছে।",
        listOf(
            "\"র‍্যাংক\" ট্যাবে আজ, এই সপ্তাহ ও সব সময়ের তালিকা দেখুন।",
            "অ্যাডমিনের ঘোষণা ডায়ালগে আসবে; আগেরগুলো প্রোফাইলের \"নোটিশ ও ঘোষণা\"-তে।",
            "সমস্যা হলে প্রোফাইল থেকে টিকেট খুলুন বা হোয়াটসঅ্যাপে যোগাযোগ করুন।",
            "উপরের প্রোফাইল আইকনে মোবাইল/পেমেন্ট নাম্বার, থিম, হোম স্ক্রিনে ইনস্টল ও লগআউট।"
        ),
        sceneCommunity()
    )
)

/* ---------- মঞ্চ: দৃশ্য চালানো ---------- */

@Composable
private fun SceneStage(scene: Scene, imgs: Map<String, ImageBitmap>, reduced: Boolean, onPhase: (Int) -> Unit) {
    val tm = rememberTextMeasurer()
    val density = LocalDensity.current
    val font = AppFonts.ui
    val frame = remember(scene) { mutableIntStateOf(if (reduced) (scene.dur * .72f).toInt() else 0) }

    LaunchedEffect(scene, reduced) {
        if (reduced) return@LaunchedEffect
        val t0 = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                frame.intValue = (((now - t0) / 1_000_000_000.0) * FPS).toInt() % scene.dur
            }
        }
    }
    LaunchedEffect(scene, reduced) {
        if (reduced) {
            onPhase(PHASE_ALL)
        } else {
            snapshotFlow { scene.phase(frame.intValue) }.distinctUntilChanged().collect { onPhase(it) }
        }
    }

    val cx = remember(tm, density, font, imgs) { Ctx(tm, density, font, imgs) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(scene.w / scene.h)
    ) {
        val k = size.width / scene.w
        scale(k, pivot = Offset.Zero) {
            scene.draw(this, cx, frame.intValue)
        }
    }
}

/* ---------- GuideDialog ---------- */

/** বন্ধ হলে (শেষ/বাদ/ব্যাক) [onClosed] ডাকে — গাইড দেখা হয়েছে মনে রাখা + নোটিশ লোড প্যারেন্টে */
@Composable
fun GuideDialog(onClosed: () -> Unit) {
    val context = LocalContext.current
    var gI by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(PHASE_NONE) }
    val reduced = remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
    }
    val imgs = mapOf(
        "a" to ImageBitmap.imageResource(R.drawable.guide_a),
        "b" to ImageBitmap.imageResource(R.drawable.guide_b),
        "c" to ImageBitmap.imageResource(R.drawable.guide_c),
        "d0" to ImageBitmap.imageResource(R.drawable.guide_d0),
        "d" to ImageBitmap.imageResource(R.drawable.guide_d),
        "e" to ImageBitmap.imageResource(R.drawable.guide_e)
    )
    val g = GUIDE[gI]
    val lastStep = gI == GUIDE.size - 1
    val gold = Color(0xFFD9AB43)

    Dialog(
        onDismissRequest = onClosed,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 460.dp)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, AppColors.extra.gold),
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Brush.linearGradient(listOf(GoldDark, Gold)))
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(g.tag, color = Brown, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0B3D33))
                            .border(1.dp, gold.copy(alpha = .55f), RoundedCornerShape(14.dp))
                    ) {
                        androidx.compose.runtime.key(gI) {
                            SceneStage(g.scene, imgs, reduced) { phase = it }
                        }
                    }
                    Text(
                        g.t, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        g.d, fontSize = 14.sp, lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    if (g.subs.isNotEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            g.subs.forEachIndexed { i, s ->
                                val on = phase == PHASE_ALL || phase == i
                                val a by animateFloatAsState(if (on) 1f else .62f, label = "sub")
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .alpha(a)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (on) gold.copy(alpha = .15f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (on) gold else Color(0xFF788C84).copy(alpha = .35f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        Modifier
                                            .padding(top = 1.dp)
                                            .size(21.dp)
                                            .clip(CircleShape)
                                            .background(Deep),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(bn(i + 1), color = Cream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(s, fontSize = 13.5.sp, lineHeight = 21.sp)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    GUIDE.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i == gI) MaterialTheme.colorScheme.primary else AppColors.extra.line)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!lastStep) TextButton(onClick = onClosed) { Text("বাদ দিন") }
                    if (gI > 0) TextButton(onClick = { phase = PHASE_NONE; gI = max(0, gI - 1) }) { Text("আগের") }
                    Button(onClick = {
                        if (lastStep) onClosed() else { phase = PHASE_NONE; gI += 1 }
                    }) { Text(if (lastStep) "শুরু করি" else "পরের") }
                }
            }
        }
    }
}
