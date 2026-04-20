package ru.escalop.app.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

data class OAuthTokens(
    val access_token: String,
    val refresh_token: String?,
    val expires_in: Long?
)

data class YandexOAuthServiceSettings(
    val authorizationUrl: String,
    val tokenUrl: String,
)

@kotlinx.serialization.Serializable
data class TokenResponse(
    @kotlinx.serialization.SerialName("access_token")
    val accessToken: String,
    @kotlinx.serialization.SerialName("refresh_token")
    val refreshToken: String? = null,
    @kotlinx.serialization.SerialName("expires_in")
    val expiresIn: Long? = null,
    @kotlinx.serialization.SerialName("token_type")
    val tokenType: String? = null,
)

class YandexOAuthService(
    private val clientId: String,
    private val redirectUri: String,
    private val httpClient: HttpClient,
    private val settings: YandexOAuthServiceSettings
) {

    suspend fun buildAuthorizeUrl(
        state: String,
        codeChallenge: String,
    ): String {
        return URLBuilder(settings.authorizationUrl).apply {
            parameters.append("response_type", "code")
            parameters.append("client_id", clientId)
            parameters.append("redirect_uri", "redirectUri")
            parameters.append("state", state)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
        }.buildString()
    }

    suspend fun exchangeCode(code: String, codeVerifier: String): OAuthTokens {
        val response: TokenResponse = httpClient.submitForm (
            url = settings.tokenUrl,
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("client_id", clientId)
                append("code_verifier", codeVerifier)
            }
        ).body()

        return OAuthTokens(
            access_token = response.accessToken,
            refresh_token = response.refreshToken,
            expires_in = response.expiresIn
        )
    }

    suspend fun refresh(refreshToken: String): OAuthTokens {
        val response: TokenResponse = httpClient.submitForm(
            url = settings.tokenUrl,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", clientId)
            }
        ).body()

        return OAuthTokens(
            access_token = response.accessToken,
            refresh_token = response.refreshToken,
            expires_in = response.expiresIn
        )
    }
}