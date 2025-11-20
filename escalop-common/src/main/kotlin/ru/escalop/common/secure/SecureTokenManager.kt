package ru.escalop.common.secure

import io.ktor.utils.io.core.*
import ru.escalop.common.model.User
import kotlin.coroutines.suspendCoroutine
import kotlin.text.toByteArray

open class SecureTokenManager {

    open suspend fun getTokenByUser(user: User): SecureToken {
        return SecureToken("y0__xC4_IEfGI65OyCzuo6JFTDQpPizCDLgofWKZTUH62OCE9w6JoGAhHXL".toByteArray())
    }
}