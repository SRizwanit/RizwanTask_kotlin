package com.srizwan.task.data

import android.content.Context
import com.srizwan.task.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONTokener
import kotlin.coroutines.cancellation.CancellationException

/**
 * Supabase REST API client — common.js-এর sb অবজেক্টের Kotlin রূপ।
 * header: apikey + Authorization: Bearer <key>
 */
class Api(private val context: Context) {
    private val url = BuildConfig.SB_URL
    private val key = BuildConfig.SB_KEY

    suspend fun get(path: String): JSONArray = request("GET", path, null, null)

    suspend fun post(path: String, body: JSONObject): JSONArray =
        request("POST", path, body, "return=representation")

    suspend fun patch(path: String, body: JSONObject): JSONArray =
        request("PATCH", path, body, "return=representation")

    suspend fun patchMin(path: String, body: JSONObject): Unit {
        request("PATCH", path, body, "return=minimal")
    }

    suspend fun upsert(path: String, body: JSONObject): JSONArray =
        request("POST", path, body, "resolution=merge-duplicates,return=representation")

    suspend fun delete(path: String): JSONArray = request("DELETE", path, null, null)

    /** POST + Prefer: return=minimal (ফলাফল লাগে না, যেমন লগ)। */
    suspend fun postMin(path: String, body: JSONObject) {
        requestText("POST", path, body, "return=minimal")
    }

    /**
     * Supabase RPC (`rpc/<fn>`)। ফাংশন সংখ্যা (scalar) বা অ্যারে ফেরত দিতে পারে,
     * তাই Any? রিটার্ন: JSONArray / Number / String / null।
     */
    suspend fun rpc(name: String, body: JSONObject = JSONObject()): Any? {
        val text = requestText("POST", "rpc/$name", body, null)
        if (text.isBlank()) return null
        return JSONTokener(text).nextValue()
    }

    /** common.js-এর getAll: ১০০০টি করে পেজিনেট করে সব সারি আনে। */
    suspend fun getAll(path: String, max: Int = 200000): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        val page = 1000
        val sep = if (path.contains("?")) "&" else "?"
        var off = 0
        while (off < max) {
            val r = get("$path${sep}limit=$page&offset=$off")
            for (i in 0 until r.length()) out.add(r.getJSONObject(i))
            if (r.length() < page) break
            off += page
        }
        return out
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject?,
        prefer: String?
    ): JSONArray {
        val text = requestText(method, path, body, prefer)
        return if (text.isBlank()) JSONArray() else JSONArray(text)
    }

    private suspend fun requestText(
        method: String,
        path: String,
        body: JSONObject?,
        prefer: String?
    ): String = withContext(Dispatchers.IO) {
        val conn = URL("$url/rest/v1/$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20000
            conn.readTimeout = 30000
            conn.setRequestProperty("apikey", key)
            conn.setRequestProperty("Authorization", "Bearer $key")
            if (body != null) conn.setRequestProperty("Content-Type", "application/json")
            if (prefer != null) conn.setRequestProperty("Prefer", prefer)
            conn.doInput = true
            if (body != null) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = if (stream == null) "" else
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (code !in 200..299) {
                throw ApiException("$method ${path.substringBefore('?')} failed: $code $text")
            }
            text
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("ইন্টারনেট সংযোগ নেই বা খুব দুর্বল — আবার চেষ্টা করুন", e)
        } finally {
            conn.disconnect()
        }
    }
}

/** URL-এ বসানোর জন্য এনকোড (common.js-এর enc)। */
fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
