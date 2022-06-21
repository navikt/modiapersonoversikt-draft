package no.nav.modiapersonoversikt

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.draft.DraftDAOImpl
import no.nav.modiapersonoversikt.draft.draftRoutes
import no.nav.modiapersonoversikt.infrastructure.*
import no.nav.modiapersonoversikt.infrastructure.HttpServer.metricsRegistry
import no.nav.modiapersonoversikt.infrastructure.Security.AzureAd
import no.nav.modiapersonoversikt.infrastructure.Security.OpenAM
import no.nav.modiapersonoversikt.utils.JacksonUtils.objectMapper
import no.nav.modiapersonoversikt.utils.minutes
import no.nav.modiapersonoversikt.utils.schedule
import org.slf4j.event.Level
import java.util.*
import javax.sql.DataSource

fun Application.draftApp(
    configuration: Configuration,
    dataSource: DataSource,
    useMock: Boolean = false
) {
    install(XForwardedHeaders)
    install(StatusPages) {
        notFoundHandler()
        exceptionHandler()
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
    }

    install(Authentication) {
        if (useMock) {
            setupMock(OpenAM, SubjectPrincipal("Z999999"))
            setupMock(AzureAd, SubjectPrincipal("Z999999"))
        } else {
            configuration.openam.let(::setupJWT)
            configuration.azuread?.let(::setupJWT)
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/modiapersonoversikt-draft/api") }
        mdc("userId", Security::getSubject)
    }

    install(MicrometerMetrics) {
        registry = metricsRegistry
    }

    val draftDAO = DraftDAOImpl(dataSource)

    Timer().schedule(delay = 5.minutes, period = 10.minutes) {
        runBlocking {
            draftDAO.deleteOldDrafts()
        }
    }

    routing {
        route("modiapersonoversikt-draft") {
            route("api") {
                draftRoutes(configuration.authproviders, draftDAO)
            }
        }
    }
}
