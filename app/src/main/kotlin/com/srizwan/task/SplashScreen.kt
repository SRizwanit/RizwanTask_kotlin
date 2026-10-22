package com.srizwan.task

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private val G1 = Color(0xFFF7E2A0)
private val G2 = Color(0xFFD9AB43)
private val G3 = Color(0xFFA8761F)
private val Gold = Color(0xFFF2D27A)
private val Pearl = Color(0xFFF3FFF9)
private val Cream = Color(0xFFFFFBE8)

private const val NAME = "RizwanTask"
private const val BISMILLAH = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"

private val DrawEase = CubicBezierEasing(0.6f, 0.1f, 0.2f, 1f)
private val RiseEase = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)
private val PopEase = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f)
private val LoadEase = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)
private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

private fun seg(ms: Long, delayMs: Long, durMs: Long, easing: Easing = LinearEasing): Float {
    val x = ((ms - delayMs).toFloat() / durMs).coerceIn(0f, 1f)
    return easing.transform(x)
}

private fun tri(ms: Long, delayMs: Long, periodMs: Long): Float {
    if (ms < delayMs) return 0f
    val p = ((ms - delayMs) % periodMs).toFloat() / periodMs
    val t = if (p < 0.5f) p * 2f else (1f - p) * 2f
    return t * t * (3f - 2f * t)
}

private fun loadValue(ms: Long): Float {
    val t = ((ms - 600L).toFloat() / 2300f).coerceIn(0f, 1f)
    return when {
        t < 0.35f -> LoadEase.transform(t / 0.35f) * 0.38f
        t < 0.70f -> 0.38f + LoadEase.transform((t - 0.35f) / 0.35f) * 0.34f
        else -> 0.72f + LoadEase.transform((t - 0.70f) / 0.30f) * 0.28f
    }
}

private fun isReducedMotion(context: Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (_: Throwable) {
    false
}

@Composable
private fun rememberClock(reduce: Boolean): State<Long> {
    val ms = remember { mutableStateOf(if (reduce) 10_000L else 0L) }
    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { ms.value = it - start }
        }
    }
    return ms
}

@Composable
fun SplashScreen(hasSession: Boolean, onFinish: (String) -> Unit) {
    val context = LocalContext.current
    val reduce = remember { isReducedMotion(context) }
    val clock = rememberClock(reduce)
    var leaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(if (reduce) 700L else 3250L)
        leaving = true
    }
    LaunchedEffect(leaving) {
        if (leaving) {
            delay(if (reduce) 0L else 420L)
            onFinish(if (hasSession) Routes.TASK else Routes.LOGIN)
        }
    }

    val outAlpha = animateFloatAsState(
        targetValue = if (leaving) 0f else 1f,
        animationSpec = tween(if (reduce) 0 else 450),
        label = "outAlpha"
    )
    val outScale = animateFloatAsState(
        targetValue = if (leaving) 1.03f else 1f,
        animationSpec = tween(if (reduce) 0 else 450),
        label = "outScale"
    )

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window == null) {
            onDispose { }
        } else {
            val ctrl = WindowCompat.getInsetsController(window, view)
            val prevStatus = ctrl.isAppearanceLightStatusBars
            val prevNav = ctrl.isAppearanceLightNavigationBars
            ctrl.isAppearanceLightStatusBars = false
            ctrl.isAppearanceLightNavigationBars = false
            onDispose {
                ctrl.isAppearanceLightStatusBars = prevStatus
                ctrl.isAppearanceLightNavigationBars = prevNav
            }
        }
    }

    val screenW = LocalConfiguration.current.screenWidthDp.toFloat()
    val markDp = minOf(screenW * 0.46f, 168f).dp
    val nameSp = (screenW * 0.11f).coerceIn(40f, 62f)
    val loadDp = minOf(screenW * 0.62f, 230f).dp
    val bsmSp = (screenW * 0.06f).coerceIn(22f, 30f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = outAlpha.value
                scaleX = outScale.value
                scaleY = outScale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { leaving = true }
    ) {
        Backdrop(clock, reduce)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = BISMILLAH,
                modifier = Modifier.graphicsLayer { alpha = seg(clock.value, 300, 1200) },
                style = TextStyle(
                    fontFamily = AppFonts.amiri,
                    fontSize = bsmSp.sp,
                    lineHeight = (bsmSp * 1.5f).sp,
                    color = G1,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Gold.copy(alpha = 0.35f), Offset.Zero, 18f)
                )
            )
            Spacer(Modifier.height(18.dp))
            LogoMark(clock, reduce, markDp)
            Spacer(Modifier.height(26.dp))
            Title(clock, reduce, nameSp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "কিতাব প্রুফিং",
                modifier = Modifier
                    .graphicsLayer { alpha = seg(clock.value, 1900, 800) }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            if (BuildConfig.DEBUG) {
                                Handler(Looper.getMainLooper()).post {
                                    throw RuntimeException("DebugActivity পরীক্ষা")
                                }
                            }
                        })
                    },
                style = TextStyle(fontSize = 15.sp, letterSpacing = 0.04.em, color = Color(0xFFA9D6C6))
            )
            Spacer(Modifier.height(16.dp))
            DividerRule(clock)
            Spacer(Modifier.height(34.dp))
            LoadBar(clock, reduce, loadDp)
        }
    }
}

private class Star(val x: Float, val y: Float, val size: Float, val dur: Long, val delay: Long)

private class LatticeAssets {
    val crescent: Path = Path.combine(
        PathOperation.Difference,
        Path().apply { addOval(Rect(Offset.Zero, 9f)) },
        Path().apply { addOval(Rect(Offset(4.2f, 0f), 8.4f)) }
    )
    val curves: Path = Path().apply {
        moveTo(52f, 8f); relativeCubicTo(6f, 6f, 6f, 14f, 0f, 20f)
        moveTo(68f, 8f); relativeCubicTo(-6f, 6f, -6f, 14f, 0f, 20f)
        moveTo(52f, 92f); relativeCubicTo(6f, 6f, 6f, 14f, 0f, 20f)
        moveTo(68f, 92f); relativeCubicTo(-6f, 6f, -6f, 14f, 0f, 20f)
    }
}

private fun DrawScope.drawEllipticalGradient(
    cx: Float, cy: Float, rx: Float, ry: Float, vararg stops: Pair<Float, Color>
) {
    withTransform({
        translate(cx, cy)
        scale(rx, ry, Offset.Zero)
    }) {
        drawRect(
            brush = Brush.radialGradient(colorStops = stops, center = Offset.Zero, radius = 1f),
            topLeft = Offset(-50f, -50f),
            size = Size(100f, 100f)
        )
    }
}

private fun DrawScope.drawRose(cx: Float, cy: Float, color: Color, stroke: Stroke) {
    val c = Offset(cx, cy)
    drawCircle(color, 2.6f, c, style = stroke)
    for (a in floatArrayOf(0f, 45f, 90f, 135f)) {
        rotate(a, c) {
            drawOval(color, Offset(cx - 3f, cy - 10.5f), Size(6f, 21f), style = stroke)
        }
    }
    drawCircle(color, 14.5f, c, style = stroke)
}

@Composable
private fun Backdrop(clock: State<Long>, reduce: Boolean) {
    val assets = remember { LatticeAssets() }
    val starCount = if (LocalConfiguration.current.screenWidthDp < 600) 26 else 46
    val stars = remember(starCount) {
        val rnd = Random(7)
        List(starCount) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                size = 1.5f + rnd.nextFloat() * 2.2f,
                dur = ((2.4f + rnd.nextFloat() * 3.6f) * 1000f).toLong(),
                delay = (rnd.nextFloat() * 4000f).toLong()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val ms = if (reduce) 0L else clock.value
        val d = density
        val w = size.width
        val h = size.height

        drawEllipticalGradient(
            w * 0.5f, h * 0.38f, w * 0.9f, h * 0.6f,
            0f to Color(0xFF0F5A47), 0.46f to Color(0xFF083A2F), 1f to Color(0xFF041A15)
        )

        val tile = 120f * d
        val shift = (ms % 70_000L) / 70_000f * tile
        val latticeColor = Gold.copy(alpha = 0.17f * 0.55f)
        val stroke = Stroke(width = 1.1f)
        var y = -tile + shift
        while (y < h) {
            var x = -tile + shift
            while (x < w) {
                withTransform({
                    translate(x, y)
                    scale(d, d, Offset.Zero)
                }) {
                    drawRose(30f, 30f, latticeColor, stroke)
                    drawRose(90f, 90f, latticeColor, stroke)
                    withTransform({
                        translate(90f, 30f)
                        rotate(-20f, Offset.Zero)
                    }) { drawPath(assets.crescent, latticeColor, style = stroke) }
                    withTransform({
                        translate(30f, 90f)
                        rotate(160f, Offset.Zero)
                    }) { drawPath(assets.crescent, latticeColor, style = stroke) }
                    drawPath(assets.curves, latticeColor, style = stroke)
                }
                x += tile
            }
            y += tile
        }

        drawEllipticalGradient(
            w * 0.5f, h * 0.45f, w * 0.7f, h * 0.55f,
            0f to Color(0x00020C09), 0.3f to Color(0x00020C09), 1f to Color(0xBF020C09)
        )

        stars.forEach { s ->
            val k = tri(ms, s.delay, s.dur)
            if (k > 0.001f) {
                val a = 0.95f * k
                val sc = 0.3f + 0.7f * k
                val cx = s.x * w
                val cy = s.y * h
                val arm = s.size * d * 3.5f * sc
                val col = G1.copy(alpha = a)
                drawCircle(col, s.size * d / 2f * sc, Offset(cx, cy))
                drawLine(col, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = d)
                drawLine(col, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = d)
            }
        }
    }
}

private class Piece(val path: Path, val color: Color, val width: Float, val delay: Long, val dur: Long) {
    val measure = PathMeasure().also { it.setPath(path, false) }
    val length = measure.length
}

private class MarkAssets {
    val plate = Path().apply {
        moveTo(9f, 61f); lineTo(9f, 35f)
        arcTo(Rect(9f, 12f, 55f, 58f), 180f, 180f, false)
        lineTo(55f, 61f); close()
    }
    val pages = Path().apply {
        moveTo(32f, 38f)
        cubicTo(28f, 35.5f, 22f, 35.5f, 17f, 37f)
        lineTo(17f, 51f)
        cubicTo(22f, 49.5f, 28f, 49.5f, 32f, 52f)
        cubicTo(36f, 49.5f, 42f, 49.5f, 47f, 51f)
        lineTo(47f, 37f)
        cubicTo(42f, 35.5f, 36f, 35.5f, 32f, 38f)
        close()
    }
    val crescent: Path = Path.combine(
        PathOperation.Difference,
        Path().apply { addOval(Rect(Offset(31.985f, 6f), 5.5f)) },
        Path().apply { addOval(Rect(Offset(35f, 6f), 4.6f)) }
    )
    val spark = Path().apply {
        moveTo(40.50f, 3.30f); lineTo(41.18f, 5.07f); lineTo(43.07f, 5.17f)
        lineTo(41.59f, 6.36f); lineTo(42.09f, 8.18f); lineTo(40.50f, 7.15f)
        lineTo(38.91f, 8.18f); lineTo(39.41f, 6.36f); lineTo(37.93f, 5.17f)
        lineTo(39.82f, 5.07f); close()
    }
    val pieces: List<Piece>

    init {
        val list = ArrayList<Piece>()
        list += Piece(
            Path().apply {
                moveTo(9f, 61f); lineTo(9f, 35f)
                arcTo(Rect(9f, 12f, 55f, 58f), 180f, 180f, false)
                lineTo(55f, 61f)
            }, Gold, 2f, 150, 1500
        )
        list += Piece(Path().apply { moveTo(4f, 61f); lineTo(60f, 61f) }, Gold, 2f, 200, 700)

        val roseDelays = longArrayOf(1100, 1200, 1300, 1400)
        val roseAngles = floatArrayOf(0f, 45f, 90f, 135f)
        list += Piece(
            Path().apply {
                addOval(Rect(Offset.Zero, 1.4f))
                transform(Matrix().apply { translate(32f, 25f) })
            }, Gold, 1.2f, 1100, 800
        )
        for (i in 0..3) {
            list += Piece(
                Path().apply {
                    addOval(Rect(-1.7f, -5f, 1.7f, 5f))
                    transform(Matrix().apply { translate(32f, 25f); rotateZ(roseAngles[i]) })
                }, Gold, 1.2f, roseDelays[i], 900
            )
        }

        list += Piece(Path().apply { moveTo(32f, 52f); lineTo(32f, 38f) }, Pearl, 2.2f, 1200, 600)
        list += Piece(
            Path().apply {
                moveTo(32f, 38f)
                cubicTo(28f, 35.5f, 22f, 35.5f, 17f, 37f)
                lineTo(17f, 51f)
                cubicTo(22f, 49.5f, 28f, 49.5f, 32f, 52f)
            }, Pearl, 2.2f, 1300, 1000
        )
        list += Piece(
            Path().apply {
                moveTo(32f, 38f)
                cubicTo(36f, 35.5f, 42f, 35.5f, 47f, 37f)
                lineTo(47f, 51f)
                cubicTo(42f, 49.5f, 36f, 49.5f, 32f, 52f)
            }, Pearl, 2.2f, 1400, 1000
        )
        list += Piece(
            Path().apply {
                moveTo(19f, 57f)
                cubicTo(25f, 56.5f, 29f, 55f, 32f, 52f)
                cubicTo(35f, 55f, 39f, 56.5f, 45f, 57f)
            }, Gold, 1.8f, 1700, 700
        )
        list += Piece(
            Path().apply {
                moveTo(36.5f, 44f)
                relativeLineTo(2.8f, 2.8f)
                relativeLineTo(4.6f, -5.8f)
            }, Gold, 2.2f, 2000, 500
        )
        pieces = list
    }
}

private fun DrawScope.drawPiece(p: Piece, ms: Long) {
    val prog = seg(ms, p.delay, p.dur, DrawEase)
    if (prog <= 0f) return
    val st = Stroke(width = p.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (prog >= 1f) {
        drawPath(p.path, p.color, style = st)
    } else {
        val part = Path()
        p.measure.getSegment(0f, p.length * prog, part, true)
        drawPath(part, p.color, style = st)
    }
}

@Composable
private fun LogoMark(clock: State<Long>, reduce: Boolean, sizeDp: Dp) {
    val assets = remember { MarkAssets() }
    Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.requiredSize(sizeDp * 1.52f)) {
            val ms = clock.value
            val loopMs = if (reduce) 0L else ms
            val s = size.minDimension
            val breathe = tri(loopMs, 1300, 3400)
            val a = seg(ms, 100, 1200) * (1f - 0.3f * breathe)
            val sc = 1f + 0.1f * breathe
            val r = s / 2f * sc
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Gold.copy(alpha = 0.32f),
                        0.62f to Gold.copy(alpha = 0f),
                        1f to Gold.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = r * 1.414f
                ),
                radius = r,
                center = center,
                alpha = a
            )
        }

        Canvas(modifier = Modifier.requiredSize(sizeDp * 1.28f)) {
            val ms = clock.value
            val loopMs = if (reduce) 0L else ms
            val s = size.minDimension
            val k = s / 100f
            val rot = (loopMs % 28_000L) / 28_000f * 360f
            rotate(rot, center) {
                drawCircle(
                    color = Gold.copy(alpha = 0.55f * seg(ms, 200, 1000)),
                    radius = 48f * k,
                    center = center,
                    style = Stroke(
                        width = 0.7f * k,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.4f * k, 3.2f * k))
                    )
                )
            }
        }

        Canvas(modifier = Modifier.size(sizeDp)) {
            val ms = clock.value
            val loopMs = if (reduce) 0L else ms
            val sc = size.minDimension / 64f
            withTransform({ scale(sc, sc, Offset.Zero) }) {
                drawPath(assets.plate, Gold.copy(alpha = 0.08f * seg(ms, 1400, 800)))
                drawPath(assets.pages, Color(0xFFEAFBF4).copy(alpha = 0.1f * seg(ms, 1500, 700)))
                assets.pieces.forEach { drawPiece(it, ms) }
                drawPath(assets.crescent, Gold.copy(alpha = seg(ms, 1600, 800)))

                val twinkling = ms >= 2900L
                val pop = seg(ms, 2100, 900, PopEase)
                val k = tri(loopMs, 2900, 2600)
                val spScale = if (twinkling) 1f - 0.4f * k else pop
                val spRot = if (twinkling) 20f * k else -90f * (1f - pop)
                val spAlpha = if (twinkling) 1f - 0.4f * k else seg(ms, 2100, 900).coerceIn(0f, 1f)
                if (spAlpha > 0f && spScale > 0f) {
                    withTransform({
                        translate(40.5f, 5.74f)
                        rotate(spRot, Offset.Zero)
                        scale(spScale, spScale, Offset.Zero)
                        translate(-40.5f, -5.74f)
                    }) {
                        drawPath(assets.spark, Color(0xFFFFF7D6).copy(alpha = spAlpha))
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(clock: State<Long>, reduce: Boolean, fontSizeSp: Float) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleFont = AppFonts.display
    val style = remember(fontSizeSp, titleFont) {
        TextStyle(
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp.sp,
            letterSpacing = 0.01.em
        )
    }
    val layouts = remember(style) { NAME.map { measurer.measure(it.toString(), style) } }
    val totalPx = layouts.sumOf { it.size.width }
    val heightPx = layouts.maxOf { it.size.height }
    val wDp = with(density) { totalPx.toDp() }
    val hDp = with(density) { heightPx.toDp() }

    Canvas(
        modifier = Modifier
            .size(wDp, hDp)
            .semantics { contentDescription = NAME }
    ) {
        val ms = clock.value
        val loopMs = if (reduce) 0L else ms
        var x = 0f
        layouts.forEachIndexed { n, lr ->
            val w = lr.size.width.toFloat()
            val enter = seg(ms, 1150L + n * 60L, 700L, RiseEase)
            val frac = if (loopMs < 2200L) 0f else ((loopMs - 2200L) % 3400L) / 3400f
            val pPercent = 130f - 260f * frac
            val offset = -1.6f * w * pPercent / 100f
            val brush = Brush.linearGradient(
                0f to G3, 0.28f to G1, 0.5f to Cream, 0.72f to G2, 1f to G3,
                start = Offset(offset, 0f),
                end = Offset(offset + 2.6f * w, 0f),
                tileMode = TileMode.Repeated
            )
            if (enter > 0f) {
                translate(left = x, top = (1f - enter) * 18f * density) {
                    drawText(lr, brush = brush, alpha = enter)
                }
            }
            x += w
        }
    }
}

@Composable
private fun DividerRule(clock: State<Long>) {
    val crescent = remember {
        Path.combine(
            PathOperation.Difference,
            Path().apply { addOval(Rect(Offset.Zero, 5.6f)) },
            Path().apply { addOval(Rect(Offset(2.4f, 0f), 4.7f)) }
        )
    }
    val star = remember {
        Path().apply {
            moveTo(4.4f, -1.6f); lineTo(5.1f, 0.1f); lineTo(6.9f, 0.2f); lineTo(5.5f, 1.3f)
            lineTo(6f, 3f); lineTo(4.4f, 2f); lineTo(2.9f, 3f); lineTo(3.4f, 1.3f)
            lineTo(2f, 0.2f); lineTo(3.8f, 0.1f); close()
        }
    }
    Row(
        modifier = Modifier.graphicsLayer { alpha = seg(clock.value, 2000, 800) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .width(54.dp)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(G2.copy(alpha = 0f), G2)))
        )
        Canvas(Modifier.size(20.dp)) {
            val k = size.minDimension / 14f
            withTransform({
                translate(7f * k, 7f * k)
                scale(k, k, Offset.Zero)
            }) {
                drawPath(crescent, G1)
                withTransform({
                    translate(0.4f, -0.2f)
                    scale(0.9f, 0.9f, Offset.Zero)
                }) { drawPath(star, G1) }
            }
        }
        Box(
            Modifier
                .width(54.dp)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(G2, G2.copy(alpha = 0f))))
        )
    }
}

@Composable
private fun LoadBar(clock: State<Long>, reduce: Boolean, widthDp: Dp) {
    val pct by remember { derivedStateOf { (loadValue(clock.value) * 100f).roundToInt() } }

    Column(
        modifier = Modifier
            .width(widthDp)
            .graphicsLayer { alpha = seg(clock.value, 600, 600) }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
        ) {
            val h = size.height
            val corner = CornerRadius(h / 2f)
            drawRoundRect(Gold.copy(alpha = 0.16f), cornerRadius = corner)
            drawRoundRect(Gold.copy(alpha = 0.2f), cornerRadius = corner, style = Stroke(width = density))
            val fillW = size.width * loadValue(clock.value)
            if (fillW > 1f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(G3, G1), startX = 0f, endX = fillW),
                    size = Size(fillW, h),
                    cornerRadius = corner
                )
                val glow = minOf(46f * density, fillW)
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Cream.copy(alpha = 0f), Cream),
                        startX = fillW - glow,
                        endX = fillW
                    ),
                    topLeft = Offset(fillW - glow, 0f),
                    size = Size(glow, h)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text(
                    text = "লোড হচ্ছে",
                    style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF8FC4B1))
                )
                for (i in 0..2) {
                    Text(
                        text = ".",
                        modifier = Modifier.graphicsLayer {
                            alpha = if (reduce) 1f else dotAlpha(clock.value, i)
                        },
                        style = TextStyle(fontSize = 12.5.sp, color = Color(0xFF8FC4B1))
                    )
                }
            }
            Text(
                text = "$pct%",
                style = TextStyle(
                    fontSize = 12.5.sp,
                    color = G1,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }
}

private fun dotAlpha(ms: Long, index: Int): Float {
    val delayMs = index * 200L
    if (ms < delayMs) return 0.2f
    val p = ((ms - delayMs) % 1200L).toFloat() / 1200f
    return when {
        p < 0.4f -> 0.2f + 0.8f * CssEase.transform(p / 0.4f)
        p < 0.8f -> 1f - 0.8f * CssEase.transform((p - 0.4f) / 0.4f)
        else -> 0.2f
    }
}
