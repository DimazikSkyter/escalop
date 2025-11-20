package ru.escalop.common.secure

class SecureToken(private val bytes: ByteArray) {

    fun asString(): String = String(bytes, Charsets.UTF_8)

    fun clear() {
        bytes.fill(0)
    }

    override fun toString(): String = "[SECURE]"
}