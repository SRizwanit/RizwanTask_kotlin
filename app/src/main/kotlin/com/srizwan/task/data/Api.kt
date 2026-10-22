package com.srizwan.task.data

import com.srizwan.task.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Supabase REST ক্লায়েন্ট (React common.js-এর sb/req-এর হুবহু রূপ)। */
object Api {
    private val SB_URL: String get() = BuildConfig.SB_URL
    private val SB_KEY: String get() = BuildConfig.SB_KEY

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private suspend fun req(method: String, path: String, body: JSONObject?, prefer: String?): JSONArray =
        withContext(Dispatchers.IO) {
            val conn: HttpURLConnection
            try {
                conn = URL("$SB_URL/rest/v1/$path").openConnection() as HttpURLConnection
                conn.requestMethod = if (method == "PATCH") "POST" else method
                if (method == "PATCH") conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.useCaches = false
                conn.setRequestProperty("apikey", SB_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SB_KEY")
                if (prefer != null) conn.setRequestProperty("Prefer", prefer)
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }
            } catch (e: IOException) {
                throw Exception("ইন্টারনেট সংযোগ নেই বা খুব দুর্বল — আবার চেষ্টা করুন")
            }
            try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) throw Exception("$method ${path.substringBefore('?')} failed: $code $text")
                when {
                    text.isBlank() -> JSONArray()
                    text.trimStart().startsWith("[") -> JSONArray(text)
                    else -> JSONArray().put(JSONObject(text))
                }
            } catch (e: IOException) {
                throw Exception("ইন্টারনেট সংযোগ নেই বা খুব দুর্বল — আবার চেষ্টা করুন")
            } finally {
                conn.disconnect()
            }
        }

    suspend fun get(path: String) = req("GET", path, null, null)
    suspend fun post(path: String, b: JSONObject) = req("POST", path, b, "return=representation")
    suspend fun patch(path: String, b: JSONObject) = req("PATCH", path, b, "return=representation")
    suspend fun postMin(path: String, b: JSONObject) = req("POST", path, b, "return=minimal")
}
