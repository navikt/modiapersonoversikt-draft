package no.nav.modiapersonoversikt.draft

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import no.nav.modiapersonoversikt.infrastructure.SubjectPrincipal

fun Route.draftRoutes(dao: DraftDAO) {
    authenticate {
        route("/draft") {
            get {
                withSubject {subject ->
                    val dto = DraftIdentificatorDTO(subject, emptyMap())
                    val result = dao.get(dto.fromDTO(), false)
                    call.respond(result.toDTO())
                }
            }

            post("/get") {
                withSubject { subject ->
                    val exact = call.request.queryParameters["exact"]?.toBoolean() ?: true

                    val dto = DraftIdentificatorDTO(subject, call.receive())
                    val result = dao.get(dto.fromDTO(), exact)

                    call.respond(result.firstOrNull()?.toDTO() ?: HttpStatusCode.NoContent)
                }
            }

            post("/search") {
                withSubject { subject ->
                    val exact = call.request.queryParameters["exact"]?.toBoolean() ?: true

                    val dto = DraftIdentificatorDTO(subject, call.receive())
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
}

private suspend fun PipelineContext<Unit, ApplicationCall>.withSubject(body: suspend (subject: String) -> Unit) {
    this.call.principal<SubjectPrincipal>()
            ?.subject
            ?.let { body(it) }
            ?: call.respond(HttpStatusCode.BadRequest)
}
