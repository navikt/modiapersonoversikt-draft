package no.nav.modiapersonoversikt

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.jackson.JacksonConverter
import io.ktor.metrics.dropwizard.DropwizardMetrics
import io.ktor.request.path
import io.ktor.routing.route
import io.ktor.routing.routing
import io.prometheus.client.dropwizard.DropwizardExports
import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.draft.DraftDAOImpl
import no.nav.modiapersonoversikt.draft.draftRoutes
import no.nav.modiapersonoversikt.infrastructure.*
import no.nav.modiapersonoversikt.utils.JacksonUtils.objectMapper
import no.nav.modiapersonoversikt.utils.schedule
import org.slf4j.event.Level
import java.util.*
import javax.sql.DataSource
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@ExperimentalTime
fun Application.draftApp(
        configuration: Configuration,
        dataSource: DataSource,
        useMock: Boolean = false
) {

    install(StatusPages) {
        notFoundHandler()
        exceptionHandler()
    }

    install(CORS) {
        anyHost()
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
    }

    install(Authentication) {
        if (useMock) {
            setupMock(SubjectPrincipal("Z999999"))
        } else {
            setupJWT(configuration.jwksUrl)
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/modiapersonoversikt-draft/api") }
        mdc("userId", Security::getSubject)
    }

    install(DropwizardMetrics) {
        io.prometheus.client.CollectorRegistry.defaultRegistry.register(DropwizardExports(registry))
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
                draftRoutes(draftDAO)
            }
        }
    }
}
