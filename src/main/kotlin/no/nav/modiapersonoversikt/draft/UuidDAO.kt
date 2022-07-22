package no.nav.modiapersonoversikt.draft

import java.time.LocalDateTime
import java.util.UUID

interface UuidDAO {
    suspend fun generateUid(owner: String): UUID
    suspend fun getOwner(uuid: UUID): String?
    suspend fun deleteExpired(): Int

    data class TableRow(
        val owner: String,
        val uuid: UUID,
        val created: LocalDateTime
    ) {
        val shouldBeRefreshed = LocalDateTime.now().isAfter(created.plusHours(1))
    }
}