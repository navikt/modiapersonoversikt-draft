package no.nav.modiapersonoversikt.draft

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.util.filter
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import no.nav.modiapersonoversikt.infrastructure.SubjectPrincipal

fun Route.draftRoutes(authProviders: Array<String>, dao: DraftDAO) {
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
