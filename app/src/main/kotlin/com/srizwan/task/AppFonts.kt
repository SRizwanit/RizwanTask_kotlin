package com.srizwan.task

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * React অ্যাপের index.html-এ যে Google Fonts লিংক আছে (Anek Bangla, Noto Serif Bengali,
 * Noto Naskh Arabic, Amiri, Cormorant Garamond) — একই ফন্টগুলো এখানে ইন্টারনেট থেকে
 * একবার নামিয়ে অ্যাপের নিজস্ব প্রাইভেট ফোল্ডারে সেভ করা হয়:
 *
 *   /data/data/<প্যাকেজ নেইম>/files/fonts/   (যেমন com.srizwan.task)
 *
 * পরের বার থেকে ইন্টারনেট ছাড়াই ফাইল থেকে লোড হয়। ডাউনলোড শেষ হলে UI নিজে থেকেই
 * নতুন ফন্টে বদলে যায় (Compose state)। ডাউনলোড না হওয়া পর্যন্ত সিস্টেম ফন্ট দেখায়।
 *
 * নূরে হুদা (Noorehuda) ফন্টটি React-এর মতোই অ্যাপের সাথে বান্ডল করা (res/font/noorehuda.ttf)।
 */
@OptIn(ExperimentalTextApi::class)
object AppFonts {

    private const val TAG = "AppFonts"

    // গুগলের CSS API পুরনো ব্রাউজার UA পেলে woff2-এর বদলে সরাসরি .ttf লিংক দেয় (Android Font এটাই পড়তে পারে)
    private const val LEGACY_UA = "Mozilla/5.0 (Linux; U; Android 2.3.6; en-us; Nexus S Build/GRK39F) " +
        "AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1"

    private class Spec(val family: String, val key: String, val weights: List<Int>)

    // React-এর index.html-এর লিংকের সাথে হুবহু একই ফন্ট ও ওজন
    private val specs = listOf(
        Spec("Anek Bangla", "anek_bangla", listOf(400, 500, 600, 700)),
        Spec("Noto Serif Bengali", "noto_serif_bengali", listOf(400, 500, 600, 700)),
        Spec("Noto Naskh Arabic", "noto_naskh_arabic", listOf(400, 500, 600)),
        Spec("Cormorant Garamond", "cormorant_garamond", listOf(600, 700)),
        Spec("Amiri", "amiri", listOf(400, 700)),
    )

    // Compose state — মান বদলালে যেসব Composable এগুলো পড়ছে সেগুলো নিজে রিকম্পোজ হয়
    private val loaded = mutableStateMapOf<String, FontFamily>()

    /** বাংলা UI ফন্ট (React: --ui → Anek Bangla) */
    val ui: FontFamily get() = loaded["anek_bangla"] ?: FontFamily.SansSerif

    /** বাংলা পড়ার ফন্ট (React: Noto Serif Bengali) */
    val bengaliSerif: FontFamily get() = loaded["noto_serif_bengali"] ?: FontFamily.Serif

    /** আরবি নাসখ ফন্ট (React: Noto Naskh Arabic) */
    val naskh: FontFamily get() = loaded["noto_naskh_arabic"] ?: FontFamily.Serif

    /** বিসমিল্লাহ/আরবি শিরোনাম (React: Amiri) */
    val amiri: FontFamily get() = loaded["amiri"] ?: FontFamily.Serif

    /** স্প্ল্যাশ টাইটেল (React: Cormorant Garamond) */
    val display: FontFamily get() = loaded["cormorant_garamond"] ?: FontFamily.Serif

    /** আরবি/উর্দু অংশ — অ্যাপের সাথে বান্ডল করা, ডাউনলোড লাগে না */
    val noorehuda: FontFamily = FontFamily(Font(R.font.noorehuda))

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    /** Application.onCreate()-এ একবার ডাকুন */
    fun start(context: Context) {
        if (started) return
        started = true
        val dir = File(context.applicationContext.filesDir, "fonts").apply { mkdirs() }
        scope.launch {
            // ১) আগে থেকে সেভ করা ফন্ট থাকলে সাথে সাথে চালু
            specs.forEach { publish(dir, it) }
            // ২) বাকিগুলো নামানো (অফলাইন হলে কিছু সময় পর আবার চেষ্টা; না হলে পরের লঞ্চে)
            val waits = longArrayOf(0, 5_000, 20_000, 60_000)
            for (wait in waits) {
                if (wait > 0) delay(wait)
                val pending = specs.filter { missing(dir, it).isNotEmpty() }
                if (pending.isEmpty()) break
                pending.forEach { spec ->
                    try {
                        download(dir, spec)
                        publish(dir, spec)
                    } catch (e: Throwable) {
                        Log.w(TAG, "${spec.family} নামানো যায়নি: ${e.message}")
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- ফাইল

    private fun fileFor(dir: File, spec: Spec, weight: Int) = File(dir, "${spec.key}_$weight.ttf")

    private fun missing(dir: File, spec: Spec): List<Int> =
        spec.weights.filter { !looksLikeFont(fileFor(dir, spec, it)) }

    private suspend fun publish(dir: File, spec: Spec) {
        val fonts = spec.weights.mapNotNull { w ->
            val f = fileFor(dir, spec, w)
            when {
                !f.exists() -> null
                looksLikeFont(f) -> Font(f, FontWeight(w))
                else -> { f.delete(); null } // নষ্ট ফাইল মুছে ফেলা হলো, আবার নামবে
            }
        }
        if (fonts.isEmpty()) return
        withContext(Dispatchers.Main) { loaded[spec.key] = FontFamily(fonts) }
    }

    private fun looksLikeFont(f: File): Boolean {
        if (!f.isFile || f.length() < 8_000) return false
        val h = ByteArray(4)
        val n = f.inputStream().use { it.read(h) }
        if (n < 4) return false
        val isTrueType = h[0] == 0.toByte() && h[1] == 1.toByte() && h[2] == 0.toByte() && h[3] == 0.toByte()
        val tag = String(h, Charsets.ISO_8859_1)
        return isTrueType || tag == "OTTO" || tag == "true" || tag == "ttcf"
    }

    // ------------------------------------------------------------ ডাউনলোড

    private fun download(dir: File, spec: Spec) {
        val need = missing(dir, spec)
        if (need.isEmpty()) return

        val css = httpText(cssUrl(spec))
        val urls = parseCss(css) // ওজন -> .ttf লিংক

        for (w in need) {
            val url = urls[w] ?: throw IllegalStateException("${spec.family} $w: লিংক পাওয়া যায়নি")
            val target = fileFor(dir, spec, w)
            val tmp = File(dir, target.name + ".part")
            try {
                httpFile(url, tmp)
                if (!looksLikeFont(tmp)) throw IllegalStateException("${spec.family} $w: ফন্ট ফাইল সঠিক নয়")
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) throw IllegalStateException("${target.name} সেভ করা যায়নি")
            } finally {
                if (tmp.exists()) tmp.delete()
            }
        }
    }

    private fun cssUrl(spec: Spec): String {
        val fam = URLEncoder.encode(spec.family, "UTF-8") // "Anek Bangla" -> "Anek+Bangla"
        return "https://fonts.googleapis.com/css2?family=$fam:wght@${spec.weights.joinToString(";")}&display=swap"
    }

    private val blockRe = Regex("@font-face\\s*\\{([^}]*)\\}")
    private val weightRe = Regex("font-weight:\\s*(\\d+)")
    private val urlRe = Regex("url\\(\\s*['\"]?([^)'\"]+)['\"]?\\s*\\)")

    /** CSS থেকে ওজন অনুযায়ী .ttf লিংক বের করে (woff2 হলে বাদ) */
    private fun parseCss(css: String): Map<Int, String> {
        val out = HashMap<Int, String>()
        for (m in blockRe.findAll(css)) {
            val body = m.groupValues[1]
            val w = weightRe.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val url = urlRe.find(body)?.groupValues?.get(1) ?: continue
            if (url.endsWith(".woff2", true) || url.endsWith(".woff", true)) continue
            // unicode-range ছাড়া ব্লক থাকলে সেটাকেই প্রাধান্য
            val whole = !body.contains("unicode-range")
            if (whole || w !in out) out[w] = url
        }
        return out
    }

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", LEGACY_UA)
        if (c.responseCode !in 200..299) {
            val code = c.responseCode
            c.disconnect()
            throw IllegalStateException("HTTP $code — $url")
        }
        return c
    }

    private fun httpText(url: String): String {
        val c = open(url)
        try {
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun httpFile(url: String, to: File) {
        val c = open(url)
        try {
            c.inputStream.use { input -> to.outputStream().use { input.copyTo(it) } }
        } finally {
            c.disconnect()
        }
    }
}
