package no.nav.modiapersonoversikt.draft

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import no.nav.modiapersonoversikt.UUIDPrincipal
import no.nav.modiapersonoversikt.log
import no.nav.modiapersonoversikt.utils.SessionList
import no.nav.personoversikt.common.ktor.utils.Security.SubjectPrincipal
import java.util.*

fun Route.draftRoutes(authProviders: Array<String?>, dao: DraftDAO, uuidDAO: UuidDAO) {
    val wsHandler = WsHandler(dao)
    val sessions = SessionList()
    application.monitor.subscribe(ApplicationStopPreparing) {
        runBlocking {
            sessions.closeAll(reason = CloseReason(CloseReason.Codes.GOING_AWAY, "GOING_AWAY"))
        }
    }


    authenticate(*authProviders) {
        route("/draft") {
            get {
                withSubject { subject ->
                    val (exact, context) = call.request.queryParameters.parse()
                    val dto = DraftIdentificatorDTO(subject, context)
                    val result = dao.get(dto.fromDTO(), exact)
                    call.respond(result.toDTO())
                }
            }

            post {
                withSubject { subject ->
                    val dto: SaveDraftDTO = call.receive()
                    val result = dao.save(dto.fromDTO(subject))
                    call.respond(result.toDTO())
                }
            }

            delete {
                withSubject { subject ->
                    val dto = DraftIdentificatorDTO(subject, call.receive())
                    dao.delete(dto.fromDTO())
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    authenticate(*authProviders) {
        get("/generate-uid") {
            withSubject { subject ->
                call.respond(uuidDAO.generateUid(subject).uuid)
            }
        }
    }
    authenticate("ws") {
        webSocket("/draft/ws") {
            sessions.track(this) {
                draftws(uuidDAO, wsHandler, this.call.principal<UUIDPrincipal>()?.uuid)
            }
        }
    }

    webSocket("/draft/ws/{uuid}") {
        sessions.track(this) {
            draftws(uuidDAO, wsHandler, call.parameters["uuid"])
        }
    }

}

suspend fun WebSocketServerSession.draftws(uuidDAO: UuidDAO, wsHandler: WsHandler, uuid: String?) {
    val ownerUuid: UuidDAO.OwnerUUID? = uuid
        ?.let {
            runCatching { UUID.fromString(it) }
                .onFailure { log.error("Received credentials but was invalid uuid: $it (${uuid})") }
                .getOrNull()
        }
        ?.let { uuidDAO.getOwner(it) }
    if (ownerUuid == null) {
        log.warn("Received credentials but could not find owner in db")
        close(CloseReason(code = 4010, message = "Unauthorized"))
    } else if (ownerUuid.shouldBeRefreshed) {
        log.warn("Received credentials but needs refreshing")
        close(CloseReason(code = 4060, message = "Refresh credentials"))
    } else {
        try {
            while (true) {
                wsHandler.process(this, ownerUuid.owner, receiveDeserialized())
            }
        } catch (e: ClosedReceiveChannelException) {
            // This is expected when client disconnectes
        } catch (e: Throwable) {
            log.error("Error in WS", e)
        }
    }
}


private fun Parameters.parse(): Pair<Boolean, DraftContext> {
    val exact = this["exact"]?.toBoolean() ?: true
    val context = this
        .filter { key, value -> "exact" != key }
        .toMap()
        .mapValues { entry -> entry.value.first() }

    return Pair(exact, context)
}

private suspend fun RoutingContext.withSubject(body: suspend (subject: String) -> Unit) {
    this.call.principal<SubjectPrincipal>()
        ?.subject
        ?.let { body(it) }
        ?: call.respond(HttpStatusCode.BadRequest)
}
