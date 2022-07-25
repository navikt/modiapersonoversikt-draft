package no.nav.modiapersonoversikt.draft

import java.time.LocalDateTime
import java.util.UUID

interface UuidDAO {
    suspend fun generateUid(owner: String): OwnerUUID
    suspend fun getOwner(uuid: UUID): OwnerUUID?
    suspend fun deleteExpired(): Int

    data class OwnerUUID(
        val owner: String,
        val uuid: UUID,
        val created: LocalDateTime
    ) {
        val shouldBeRefreshed = LocalDateTime.now().isAfter(created.plusHours(1))
    }
}