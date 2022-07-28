package no.nav.modiapersonoversikt.draft

import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import no.nav.modiapersonoversikt.log
import no.nav.personoversikt.ktor.utils.Security.SubjectPrincipal
import java.util.*

fun Route.draftRoutes(authProviders: Array<String?>, dao: DraftDAO, uuidDAO: UuidDAO) {
    val wsHandler = WsHandler(dao)

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
                call.respond(uuidDAO.generateUid(subject).uuid.toString())
            }
        }
    }

    webSocket("/draft/ws/{uuid}") {
        val uuid = call.parameters["uuid"]
        val ownerUuid: UuidDAO.OwnerUUID? = uuid
            ?.let {
                runCatching { UUID.fromString(it) }
                    .onFailure { log.error("Received credentials but was invalid uuid: $it") }
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
                    wsHandler.process(ownerUuid.owner, receiveDeserialized())
                }
            } catch (e: ClosedReceiveChannelException) {
                // This is expected when client disconnectes
            } catch (e: Throwable) {
                log.error("Error in WS", e)
            }
        }
    }
}

suspend inline fun <reified T> WebSocketServerSession.deserialize(frame: Frame.Text): T {
    val conv = checkNotNull(converter) { "No converter found" }
    val result = conv.deserialize(
        charset = call.request.headers.suitableCharset(),
        typeInfo = typeInfo<T>(),
        content = frame
    )
    if (result is T) return result

    throw WebsocketDeserializeException(
        "Can't deserialize value : expected value of type ${T::class.simpleName}, got ${result::class.simpleName}",
        frame = frame
    )
}

private fun Parameters.parse(): Pair<Boolean, DraftContext> {
    val exact = this["exact"]?.toBoolean() ?: true
    val context = this
        .filter { key, value -> "exact" != key }
        .toMap()
        .mapValues { entry -> entry.value.first() }

    return Pair(exact, context)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.withSubject(body: suspend (subject: String) -> Unit) {
    this.call.principal<SubjectPrincipal>()
        ?.subject
        ?.let { body(it) }
        ?: call.respond(HttpStatusCode.BadRequest)
}
