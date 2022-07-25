package no.nav.modiapersonoversikt.draft

class WsHandler(val dao: DraftDAO) {
    enum class EventType { UPDATE, DELETE; }
    data class Message(
        val type: EventType,
        val context: DraftContext,
        val content: String?
    )

    suspend fun process(owner: String, message: Message) {
        when (message.type) {
            EventType.UPDATE -> {
                dao.save(
                    SaveDraft(
                        owner = owner,
                        context = message.context,
                        content = message.content ?: ""
                    )
                )
            }
            EventType.DELETE -> {
                dao.delete(
                    DraftIdentificator(
                        owner = owner,
                        context = message.context
                    )
                )
            }
        }
    }
}