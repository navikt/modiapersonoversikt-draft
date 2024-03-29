package no.nav.modiapersonoversikt.infrastructure

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.modiapersonoversikt.WithDatabase
import no.nav.modiapersonoversikt.withTestApp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HttpServerTest : WithDatabase {
    @Test
    fun `nais-app should have isAlive, isReady, metrics`() {
        withTestApp(connectionUrl()) {
            val client = createClient {
                install(HttpRequestRetry)
            }
            val isAlive = client.get("/modiapersonoversikt-draft/internal/isAlive")
            assertEquals(isAlive.status.value, 200)
            assertEquals(isAlive.bodyAsText(), "Alive")

            val isReady = client.get("/modiapersonoversikt-draft/internal/isReady") {
                // Use retries in case selftest-probes hasn't been run yet
                retry {
                    retryOnServerErrors(3)
                }
            }

            assertEquals(isReady.status.value, 200)
            assertEquals(isReady.bodyAsText(), "Ready")

            val metrics = client.get("/modiapersonoversikt-draft/internal/metrics")
            assertEquals(metrics.status.value, 200)
        }
    }
}
