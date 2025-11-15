package ru.escalop.ru.escalop.common.secure

import ru.escalop.ru.escalop.common.model.User
import kotlin.coroutines.suspendCoroutine

open class SecureTokenManager {

    open suspend fun getTokenByUser(user: User): SecureToken {
        return suspendCoroutine { continuation ->}
    }
}