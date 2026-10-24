package com.srizwan.task.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PIN hashing: sha256("salt:pin") hex (common.js-এর মতো)
 */
object PinHash {
    suspend fun hash(pin: String, salt: String): String = withContext(Dispatchers.Default) {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun verify(user: JSONObject, pin: String): Boolean {
        val salt = user.optString("password_salt", "")
        val hash = user.optString("password_hash", "")
        if (salt.isEmpty() || hash.isEmpty()) return false
        return hash(pin, salt) == hash
    }

    fun hasPin(user: JSONObject): Boolean {
        return user.optString("password_hash", "").isNotEmpty() &&
                user.optString("password_salt", "").isNotEmpty()
    }
}
