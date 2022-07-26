package no.nav.modiapersonoversikt

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

class OidcTest(val url: String) {
    suspend fun fetchConfig(): String {
        val httpProxy = System.getenv("HTTP_PROXY")
        val httpClient = HttpClient(CIO) {
            engine {
                val httpProxy = System.getenv("HTTP_PROXY")
                log.info("OidcWellKnownUrl will use proxy: $httpProxy")
                httpProxy?.let { proxy = ProxyBuilder.http(Url(it)) }
            }
        }
        return httpClient
            .runCatching { get(URL(url)).bodyAsText() }
            .onFailure { log.error("Could not fetch oidc-config from $url through $httpProxy", it) }
            .getOrThrow()
    }
}
