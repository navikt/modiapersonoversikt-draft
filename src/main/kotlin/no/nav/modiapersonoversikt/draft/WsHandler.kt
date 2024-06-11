package no.nav.modiapersonoversikt.draft

import io.ktor.server.websocket.*
import java.time.LocalDateTime

class WsHandler(val dao: DraftDAO) {
    enum class EventType { UPDATE, DELETE }
    data class Message(
        val type: EventType,
        val context: DraftContext,
        val content: String?
    )

    data class ConfirmMessage(
        val type: String = "OK",
        val time: LocalDateTime = LocalDateTime.now()
    )

    suspend fun process(wsSession: WebSocketServerSession,  owner: String, message: Message) {
        when (message.type) {
            EventType.UPDATE -> {
                val draft = dao.save(
                    SaveDraft(
                        owner = owner,
                        context = message.context,
                        content = message.content ?: ""
                    )
                )
                wsSession.sendSerialized(ConfirmMessage(time = draft.created))
            }
            EventType.DELETE -> {
                dao.delete(
                    DraftIdentificator(
                        owner = owner,
                        context = message.context
                    )
                )
                wsSession.sendSerialized(ConfirmMessage())
            }
        }
    }
}