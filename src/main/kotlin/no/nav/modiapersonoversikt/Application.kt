package no.nav.modiapersonoversikt

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.config.Configuration
import no.nav.modiapersonoversikt.draft.DraftDAOImpl
import no.nav.modiapersonoversikt.draft.UuidDAOImpl
import no.nav.modiapersonoversikt.draft.draftRoutes
import no.nav.modiapersonoversikt.infrastructure.UUIDPrincipal
import no.nav.modiapersonoversikt.infrastructure.exceptionHandler
import no.nav.modiapersonoversikt.infrastructure.notFoundHandler
import no.nav.modiapersonoversikt.utils.JacksonUtils.objectMapper
import no.nav.personoversikt.ktor.utils.Metrics
import no.nav.personoversikt.ktor.utils.Security
import no.nav.personoversikt.ktor.utils.Selftest
import org.slf4j.event.Level
import java.util.*
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes

fun Application.draftApp(
    configuration: Configuration,
    dataSource: DataSource,
    useMock: Boolean = false
) {
    val security = Security(
        listOfNotNull(
            configuration.openam,
            configuration.azuread,
        )
    )
    val draftDAO = DraftDAOImpl(dataSource)
    val uuidDAO = UuidDAOImpl(dataSource)

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

    install(Metrics.Plugin) {
        contextpath = appContextpath
    }

    install(Selftest.Plugin) {
        appname = appName
        contextpath = appContextpath
        version = appImage
    }

    install(Authentication) {
        if (useMock) {
            security.setupMock(this, "Z999999")
        } else {
            security.setupJWT(this)
        }
        basic("ws") {
            validate {
                UUIDPrincipal(UUID.fromString(it.name))
            }
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(WebSockets) {
        contentConverter = JacksonWebsocketContentConverter(objectMapper)
    }

    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/modiapersonoversikt-draft/api") }
        mdc("userId") { call -> security.getSubject(call).joinToString(";") }
    }

    fixedRateTimer(
        daemon = true,
        initialDelay = 5.minutes.inWholeMilliseconds,
        period = 10.minutes.inWholeMilliseconds
    ) {
        runBlocking {
            draftDAO.deleteOldDrafts()
            uuidDAO.deleteExpired()
        }
    }

    routing {
        route(appContextpath) {
            route("api") {
                draftRoutes(security.authproviders, draftDAO, uuidDAO)
            }
        }
    }
}
