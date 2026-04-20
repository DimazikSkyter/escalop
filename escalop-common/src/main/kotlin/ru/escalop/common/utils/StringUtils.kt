package ru.escalop.common.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

fun generateRandomString(length: Int = 64): String {
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(bytes)
        .take(length)
}

fun codeChallengeS256(codeVerifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(codeVerifier.toByteArray())
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(digest)
}