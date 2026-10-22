package com.srizwan.task.data

import java.security.MessageDigest
import java.security.SecureRandom

/** PIN হ্যাশ: sha256("salt:pin") hex — React ও পুরোনো অ্যাপের সাথে মিলে। */
object PinHash {
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    fun sha256(s: String): String = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)))
    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.let(::hex)
    fun hashPin(pin: String, salt: String) = sha256("$salt:$pin")
    fun hasPin(u: AppUser) = !u.passwordHash.isNullOrEmpty() && !u.passwordSalt.isNullOrEmpty()
    fun verifyPin(u: AppUser, pin: String): Boolean {
        if (u.passwordSalt.isNullOrEmpty() || u.passwordHash.isNullOrEmpty()) return false
        return hashPin(pin, u.passwordSalt) == u.passwordHash
    }
}
