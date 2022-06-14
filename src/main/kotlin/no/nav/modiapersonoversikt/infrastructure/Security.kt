package no.nav.modiapersonoversikt.infrastructure

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.*
import no.nav.modiapersonoversikt.config.AuthProviderConfig
import no.nav.modiapersonoversikt.log
import java.net.URL
import java.util.concurrent.TimeUnit

fun AuthenticationConfig.setupMock(name: String? = null, principal: SubjectPrincipal) {
    val config = object : AuthenticationProvider.Config(name) {}
    register(
        object : AuthenticationProvider(config) {
            override suspend fun onAuthenticate(context: AuthenticationContext) {
                context.principal = principal
            }
        }
    )
}

fun AuthenticationConfig.setupJWT(config: AuthProviderConfig) {
    jwt(config.name) {
        if (config.usesCookies) {
            authHeader {
                Security.getToken(it)?.let(::parseAuthorizationHeader)
            }
        }
        verifier(Security.makeJwkProvider(config.jwksUrl))
        validate { Security.validateJWT(it) }
    }
}

object Security {
    const val OpenAM = "openam"
    const val AzureAd = "azuread"
    private val cookieNames = listOf("modia_ID_token", "ID_token")

    fun getSubject(call: ApplicationCall): String {
        return try {
            getToken(call)
                ?.let { JWT.decode(it).getNavSubject() }
                ?: "Unauthenticated"
        } catch (e: Throwable) {
            "Invalid JWT"
        }
    }

    internal fun makeJwkProvider(jwksUrl: String): JwkProvider =
        JwkProviderBuilder(URL(jwksUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    internal fun validateJWT(credentials: JWTCredential): Principal? {
        return try {
            requireNotNull(credentials.payload.audience) { "Audience not present" }
            SubjectPrincipal(requireNotNull(credentials.payload.getNavSubject()))
        } catch (e: Exception) {
            log.error("Failed to validateJWT token", e)
            null
        }
    }

    internal fun getToken(call: ApplicationCall): String? {
        val headerToken = call.request.header(HttpHeaders.Authorization)
        if (headerToken != null) {
            return headerToken
        }
        return cookieNames
            .find { !call.request.cookies[it].isNullOrEmpty() }
            ?.let { call.request.cookies[it] }
            ?.let {
                if (it.startsWith("bearer", ignoreCase = true)) {
                    it
                } else {
                    "Bearer $it"
                }
            }
    }

    internal fun Payload.getNavSubject(): String? {
        return getClaim("NAVident")?.asString() ?: subject
    }

    private fun HttpAuthHeader.getBlob() = when {
        this is HttpAuthHeader.Single -> blob
        else -> null
    }
}

class SubjectPrincipal(val subject: String) : Principal
