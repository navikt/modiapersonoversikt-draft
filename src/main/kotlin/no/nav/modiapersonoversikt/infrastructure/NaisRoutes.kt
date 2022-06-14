package no.nav.modiapersonoversikt.infrastructure

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.prometheus.client.CollectorRegistry
import no.nav.modiapersonoversikt.infrastructure.HttpServer.metricsRegistry

fun Route.naisRoutes(
    readinessCheck: () -> Boolean,
    livenessCheck: () -> Boolean = { true },
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
) {
    route("internal") {
        get("/isAlive") {
            if (livenessCheck()) {
                call.respondText("Alive")
            } else {
                call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/isReady") {
            if (readinessCheck()) {
                call.respondText("Ready")
            } else {
                call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/metrics") {
            call.respondText(metricsRegistry.scrape())
        }
    }
}
