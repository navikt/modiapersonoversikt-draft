package no.nav.modiapersonoversikt.infrastructure

import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import no.nav.modiapersonoversikt.WithDatabase
import no.nav.modiapersonoversikt.withTestApp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HttpServerTest : WithDatabase {
    @Test
    fun `nais-app should have isAlive, isReady, metrics`() {
        withTestApp {
            handleRequest(HttpMethod.Get, "/modiapersonoversikt-draft/internal/isAlive").apply {
                assertEquals(response.status()?.value, 200)
                assertEquals(response.content, "Alive")
            }

            handleRequest(HttpMethod.Get, "/modiapersonoversikt-draft/internal/isReady").apply {
                assertEquals(response.status()?.value, 200)
                assertEquals(response.content, "Ready")
            }

            handleRequest(HttpMethod.Get, "/modiapersonoversikt-draft/internal/metrics").apply {
                assertEquals(response.status()?.value, 200)
            }
        }
    }
}
