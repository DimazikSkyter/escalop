package ru.escalop.common.secure

import io.ktor.utils.io.core.*
import ru.escalop.common.model.User
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.suspendCoroutine
import kotlin.text.toByteArray

open class SecureTokenManager {

    private val accessTokens = ConcurrentHashMap<String, SecureToken>()
    private val refreshTokens = ConcurrentHashMap<String, SecureToken>()


    open fun saveToken(user: User,
                       accessToken: String,
                       refreshToken: String?) {
        accessTokens[user.name] = SecureToken(accessToken.toByteArray(Charsets.UTF_8))
        refreshToken?.let {
            refreshTokens[user.name] = SecureToken(it.toByteArray(Charsets.UTF_8))
        }
    }

    open fun getTokenByUser(user: User): SecureToken {
        return accessTokens[user.name]
            ?: error("Token for user ${user.name} not found")
    }

    fun getRefreshToken(user: User): SecureToken? {
        return refreshTokens[user.name]
    }

    fun clearToken(user: User) {
        accessTokens.remove(user.name)
        refreshTokens.remove(user.name)
    }
}