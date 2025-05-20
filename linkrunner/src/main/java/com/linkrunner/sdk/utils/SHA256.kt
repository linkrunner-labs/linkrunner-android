package com.linkrunner.sdk.utils

import java.security.MessageDigest

/**
 * Simple SHA-256 hashing utility for LinkrunnerSDK
 */
object SHA256 {
    /**
     * Hashes data using SHA-256 algorithm
     * @param data The string to hash
     * @return The hashed string in hexadecimal format
     */
    fun hash(data: String): String {
        val bytes = data.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
