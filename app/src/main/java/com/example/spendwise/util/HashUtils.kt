package com.example.spendwise.util

import java.security.MessageDigest

object HashUtils {
    /**
     * Generates a SHA-256 hexadecimal hash string for the given input.
     * Prevents storing plaintext passwords in local databases.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies plain text password against stored hash (or legacy plain text transition).
     */
    fun verifyPassword(plain: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || plain.isBlank()) return false
        val computedHash = sha256(plain)
        if (computedHash.equals(storedHash, ignoreCase = true)) return true
        // Legacy fallback if an account had unhashed password during early development
        return plain == storedHash
    }
}
