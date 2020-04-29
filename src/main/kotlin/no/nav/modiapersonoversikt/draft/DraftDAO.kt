package no.nav.modiapersonoversikt.draft

import java.time.LocalDateTime

data class DraftIdentificatorDTO(
        val owner: String,
        val context: DraftContext
)

data class SaveDraftDTO(
        val content: String,
        val context: DraftContext
)

data class DraftDTO(
        val owner: String,
        val content: String,
        val context: DraftContext,
        val created: LocalDateTime
)

data class DraftIdentificator(
        val owner: String,
        val context: DraftContext
)

data class SaveDraft(
        val owner: String,
        val content: String,
        val context: DraftContext
)
typealias DraftContext = Map<String, String>

data class Draft(
        val owner: String,
        val content: String,
        val context: DraftContext,
        val created: LocalDateTime
)

fun DraftIdentificatorDTO.fromDTO() = DraftIdentificator(owner, context)
fun SaveDraftDTO.fromDTO(owner: String) = SaveDraft(owner, content, context)
fun Draft.toDTO() = DraftDTO(owner, content, context, created)
fun SaveDraft.toDraftIdentificator() = DraftIdentificator(owner, context)

interface DraftDAO {
    suspend fun save(data: SaveDraft): Draft
    suspend fun get(data: DraftIdentificator): Draft?
    suspend fun getAll(data: DraftIdentificator): List<Draft>
    suspend fun delete(data: DraftIdentificator)
}
