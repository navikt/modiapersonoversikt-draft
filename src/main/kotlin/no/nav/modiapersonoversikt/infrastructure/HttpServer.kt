package no.nav.modiapersonoversikt.infrastructure

import io.ktor.server.application.Application
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.modiapersonoversikt.log

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)
object HttpServer {
    val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun create(appname: String, port: Int, module: Application.() -> Unit): ApplicationEngine {
        val applicationState = ApplicationState()

        val applicationServer = embeddedServer(Netty, port) {
            naisApplication(appname, applicationState, module)
        }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Shutdown hook called, shutting down gracefully")
                applicationState.initialized = false
                applicationServer.stop(5000, 5000)
            }
        )

        return applicationServer
    }
}

fun Application.naisApplication(appname: String, applicationState: ApplicationState, module: Application.() -> Unit) {
    routing {
        route(appname) {
            naisRoutes(readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
        }
    }
    module(this)
    applicationState.initialized = true
}
